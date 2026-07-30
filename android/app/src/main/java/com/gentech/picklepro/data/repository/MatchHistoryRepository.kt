package com.gentech.picklepro.data.repository

import android.content.Context
import com.gentech.picklepro.data.remote.SupabaseModule
import com.gentech.picklepro.data.remote.dto.MatchResultDto
import com.gentech.picklepro.data.remote.dto.ProfileDto
import com.gentech.picklepro.data.remote.dto.RegistrationDto
import com.gentech.picklepro.data.remote.dto.TeamDto
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.query.Order

enum class MatchOutcome { WIN, LOSS }

/** One row in the profile's match history list (spec §4.2). */
data class MatchHistoryItem(
    val matchId: String,
    val tournamentName: String,
    val divisionName: String,
    val opponentName: String,
    /** Game scores oriented (mine, theirs) per game, in play order. */
    val games: List<Pair<Int, Int>>,
    val outcome: MatchOutcome,
    val finishedAt: String?,
)

/**
 * Resolves the player's finished matches into a display-ready list.
 * `side_a_ref`/`side_b_ref` on `matches` are polymorphic — a
 * registrations.id for singles or a teams.id for doubles/mixed — so this
 * repository first finds all refs that belong to the player, then all refs
 * on the *other* side of those matches, then resolves both to names.
 */
class MatchHistoryRepository(context: Context) {

    private val client = SupabaseModule.get(context)

    suspend fun fetchRecent(playerId: String, limit: Int = 20): List<MatchHistoryItem> {
        val myRegistrations = client.postgrest.from("registrations")
            .select(Columns.list("id", "division_id", "player_id", "team_id")) {
                filter { eq("player_id", playerId) }
            }
            .decodeList<RegistrationDto>()

        val myTeams = client.postgrest.from("teams")
            .select(Columns.list("id", "division_id", "player1_id", "player2_id")) {
                filter {
                    or {
                        eq("player1_id", playerId)
                        eq("player2_id", playerId)
                    }
                }
            }
            .decodeList<TeamDto>()

        val myRefs = (myRegistrations.map { it.id } + myTeams.map { it.id }).toSet()
        if (myRefs.isEmpty()) return emptyList()

        val matches = client.postgrest.from("matches")
            .select(
                Columns.raw(
                    "id, side_a_ref, side_b_ref, winner_ref, games, finished_at, " +
                        "divisions(name, event_type, tournaments(name))",
                ),
            ) {
                filter {
                    eq("status", "done")
                    or {
                        isIn("side_a_ref", myRefs.toList())
                        isIn("side_b_ref", myRefs.toList())
                    }
                }
                order("finished_at", Order.DESCENDING)
                limit(limit.toLong())
            }
            .decodeList<MatchResultDto>()

        if (matches.isEmpty()) return emptyList()

        val opponentRefs = matches.mapNotNull { m ->
            val a = m.sideARef
            val b = m.sideBRef
            when {
                a != null && a in myRefs -> b
                b != null && b in myRefs -> a
                else -> null
            }
        }.filterNotNull().toSet()

        val opponentRegs = if (opponentRefs.isEmpty()) emptyList() else client.postgrest
            .from("registrations")
            .select(Columns.list("id", "division_id", "player_id", "team_id")) {
                filter { isIn("id", opponentRefs.toList()) }
            }
            .decodeList<RegistrationDto>()

        val opponentTeams = if (opponentRefs.isEmpty()) emptyList() else client.postgrest
            .from("teams")
            .select(Columns.list("id", "division_id", "player1_id", "player2_id")) {
                filter { isIn("id", opponentRefs.toList()) }
            }
            .decodeList<TeamDto>()

        val neededPlayerIds = (opponentRegs.map { it.playerId } +
            opponentTeams.flatMap { listOfNotNull(it.player1Id, it.player2Id) }).toSet()

        val profilesById = if (neededPlayerIds.isEmpty()) emptyMap() else client.postgrest
            .from("profiles")
            .select(Columns.list("id", "name")) { filter { isIn("id", neededPlayerIds.toList()) } }
            .decodeList<ProfileDto>()
            .associateBy { it.id }

        val nameByRef = buildMap {
            opponentRegs.forEach { reg ->
                put(reg.id, profilesById[reg.playerId]?.name ?: "Kalaban")
            }
            opponentTeams.forEach { team ->
                val names = listOfNotNull(
                    profilesById[team.player1Id]?.name,
                    team.player2Id?.let { profilesById[it]?.name },
                )
                put(team.id, if (names.isEmpty()) "Kalaban" else names.joinToString(" / "))
            }
        }

        return matches.map { m ->
            val iAmSideA = m.sideARef != null && m.sideARef in myRefs
            val myRef = if (iAmSideA) m.sideARef else m.sideBRef
            val opponentRef = if (iAmSideA) m.sideBRef else m.sideARef
            MatchHistoryItem(
                matchId = m.id,
                tournamentName = m.divisions.tournaments.name,
                divisionName = m.divisions.name,
                opponentName = nameByRef[opponentRef] ?: "Kalaban",
                games = m.games.map { g -> if (iAmSideA) g.a to g.b else g.b to g.a },
                outcome = if (m.winnerRef == myRef) MatchOutcome.WIN else MatchOutcome.LOSS,
                finishedAt = m.finishedAt,
            )
        }
    }
}
