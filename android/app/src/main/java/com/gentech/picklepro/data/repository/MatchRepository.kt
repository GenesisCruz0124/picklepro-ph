package com.gentech.picklepro.data.repository

import android.content.Context
import com.gentech.picklepro.core.scoring.GameState
import com.gentech.picklepro.core.scoring.ScoreEngine
import com.gentech.picklepro.core.scoring.ScoreEvent
import com.gentech.picklepro.core.scoring.ScoringMode
import com.gentech.picklepro.core.scoring.Team
import com.gentech.picklepro.data.local.PickleProDatabase
import com.gentech.picklepro.data.local.decodeEventLog
import com.gentech.picklepro.data.local.encodeEventLog
import com.gentech.picklepro.data.local.entity.MatchEntity
import com.gentech.picklepro.data.local.entity.PendingOpEntity
import com.gentech.picklepro.data.remote.SupabaseModule
import com.gentech.picklepro.data.remote.dto.GameScoreDto
import com.gentech.picklepro.data.remote.dto.MatchRemoteDto
import com.gentech.picklepro.data.sync.AdvanceBracketPayload
import com.gentech.picklepro.data.sync.SyncOpType
import com.gentech.picklepro.data.sync.SyncScheduler
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

data class ScoringConfig(
    val isDoubles: Boolean,
    val mode: ScoringMode,
    val gameTo: Int,
    val winBy2: Boolean,
    val bestOf: Int,
    /** Round robin has no bracket tree to advance through — every match stands alone. */
    val isSingleElimination: Boolean,
)

private val matchJson = Json { ignoreUnknownKeys = true }

/**
 * Offline-first match scoring (spec §5.6, §5.10). [MatchEntity] in Room is
 * the source of truth — every mutation writes there first and enqueues a
 * sync op; nothing here ever waits on a network call.
 */
class MatchRepository(private val appContext: Context) {

    private val client = SupabaseModule.get(appContext)
    private val matchDao = PickleProDatabase.get(appContext).matchDao()
    private val pendingOpDao = PickleProDatabase.get(appContext).pendingOpDao()

    fun observe(matchId: String): Flow<MatchEntity?> = matchDao.observe(matchId)

    suspend fun get(matchId: String): MatchEntity? = matchDao.get(matchId)

    /** Loads the match into Room the first time this device scores/views it. */
    suspend fun ensureLoaded(matchId: String) {
        if (matchDao.get(matchId) != null) return
        val remote = client.postgrest.from("matches")
            .select(Columns.ALL) { filter { eq("id", matchId) } }
            .decodeSingle<MatchRemoteDto>()
        matchDao.upsert(remote.toEntity())
    }

    fun currentGameState(match: MatchEntity, config: ScoringConfig): GameState {
        val events = decodeEventLog(match.pointLogJson)
            .filter { it.first == match.currentGameNumber }
            .map { it.second }
        return ScoreEngine.replay(events, config.mode, config.isDoubles)
    }

    fun gameWinners(match: MatchEntity): List<Team> =
        decodeGames(match.gamesJson).map { if (it.a > it.b) Team.A else Team.B }

    suspend fun recordRally(matchId: String, winner: Team, config: ScoringConfig) {
        mutate(matchId, config) { events, gameNumber -> events + (gameNumber to ScoreEvent.RallyWon(winner)) }
    }

    suspend fun recordTimeout(matchId: String, team: Team, config: ScoringConfig) {
        mutate(matchId, config) { events, gameNumber -> events + (gameNumber to ScoreEvent.Timeout(team)) }
    }

    /** Drops the last event in the *current* game only — undo never reopens a completed game. */
    suspend fun undo(matchId: String, config: ScoringConfig) {
        mutate(matchId, config) { events, gameNumber ->
            val lastIndexInCurrentGame = events.indexOfLast { it.first == gameNumber }
            if (lastIndexInCurrentGame < 0) events else events.toMutableList().apply { removeAt(lastIndexInCurrentGame) }
        }
    }

    private suspend fun mutate(
        matchId: String,
        config: ScoringConfig,
        transform: (events: List<Pair<Int, ScoreEvent>>, currentGameNumber: Int) -> List<Pair<Int, ScoreEvent>>,
    ) {
        val match = matchDao.get(matchId) ?: return
        if (match.status == "done") return

        val allEvents = decodeEventLog(match.pointLogJson)
        val updatedEvents = transform(allEvents, match.currentGameNumber)
        val currentGameEvents = updatedEvents.filter { it.first == match.currentGameNumber }.map { it.second }
        val state = ScoreEngine.replay(currentGameEvents, config.mode, config.isDoubles)

        val gameWinner = ScoreEngine.gameWinner(state, config.gameTo, config.winBy2)
        var games = decodeGames(match.gamesJson)
        var nextGameNumber = match.currentGameNumber
        var matchWinnerTeam: Team? = null
        var status = match.status

        if (gameWinner != null) {
            games = games + GameScoreDto(state.scoreA, state.scoreB)
            nextGameNumber += 1
            val priorWinners = games.map { if (it.a > it.b) Team.A else Team.B }
            matchWinnerTeam = ScoreEngine.matchWinner(priorWinners, config.bestOf)
            if (matchWinnerTeam != null) status = "done"
        }

        val winnerRef = matchWinnerTeam?.let { if (it == Team.A) match.sideARef else match.sideBRef }
        val nowIso = java.time.Instant.now().toString()

        val updated = match.copy(
            status = status,
            winnerRef = winnerRef ?: match.winnerRef,
            gamesJson = matchJson.encodeToString(games),
            pointLogJson = encodeEventLog(updatedEvents),
            currentGameNumber = nextGameNumber,
            startedAt = match.startedAt ?: nowIso,
            finishedAt = if (status == "done") nowIso else match.finishedAt,
            synced = false,
        )
        matchDao.upsert(updated)
        enqueueUpsert(updated)

        if (status == "done" && match.status != "done") {
            val justCompletedWinnerRef = winnerRef
            if (config.isSingleElimination && justCompletedWinnerRef != null) {
                pendingOpDao.enqueue(
                    PendingOpEntity(
                        opType = SyncOpType.ADVANCE_BRACKET,
                        payloadJson = matchJson.encodeToString(
                            AdvanceBracketPayload(
                                divisionId = updated.divisionId,
                                round = updated.round,
                                position = updated.position,
                                winnerRef = justCompletedWinnerRef,
                            ),
                        ),
                        createdAt = System.currentTimeMillis(),
                    ),
                )
            }
            pendingOpDao.enqueue(
                PendingOpEntity(
                    opType = SyncOpType.PROCESS_MATCH_RESULT,
                    payloadJson = matchJson.encodeToString(mapOf("matchId" to updated.id)),
                    createdAt = System.currentTimeMillis(),
                ),
            )
        }
        SyncScheduler.requestSync(appContext)
    }

    private suspend fun enqueueUpsert(match: MatchEntity) {
        val remote = match.toRemoteDto()
        pendingOpDao.enqueue(
            PendingOpEntity(
                opType = SyncOpType.UPSERT_MATCH,
                payloadJson = matchJson.encodeToString(remote),
                createdAt = System.currentTimeMillis(),
            ),
        )
    }

    /** Used by bracket-advancement (a completed match filling the next round's slot). */
    suspend fun enqueueMatchUpdate(match: MatchEntity) {
        matchDao.upsert(match)
        enqueueUpsert(match)
        SyncScheduler.requestSync(appContext)
    }
}

private fun decodeGames(json: String): List<GameScoreDto> =
    if (json.isBlank()) emptyList() else matchJson.decodeFromString(json)

fun MatchRemoteDto.toEntity(): MatchEntity = MatchEntity(
    id = id,
    divisionId = divisionId,
    round = round,
    position = position,
    court = court,
    sideARef = sideARef,
    sideBRef = sideBRef,
    status = status,
    winnerRef = winnerRef,
    gamesJson = matchJson.encodeToString(games),
    pointLogJson = matchJson.encodeToString(pointLog),
    currentGameNumber = games.size + 1,
    startedAt = startedAt,
    finishedAt = finishedAt,
    synced = true,
)

fun MatchEntity.toRemoteDto(): MatchRemoteDto = MatchRemoteDto(
    id = id,
    divisionId = divisionId,
    round = round,
    position = position,
    court = court,
    sideARef = sideARef,
    sideBRef = sideBRef,
    status = status,
    winnerRef = winnerRef,
    games = decodeGames(gamesJson),
    pointLog = matchJson.decodeFromString(pointLogJson.ifBlank { "[]" }),
    startedAt = startedAt,
    finishedAt = finishedAt,
)
