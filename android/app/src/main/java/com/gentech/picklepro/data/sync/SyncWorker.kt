package com.gentech.picklepro.data.sync

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.gentech.picklepro.data.local.PickleProDatabase
import com.gentech.picklepro.data.local.entity.PendingOpEntity
import com.gentech.picklepro.data.remote.SupabaseModule
import com.gentech.picklepro.data.remote.dto.MatchAdvanceSideADto
import com.gentech.picklepro.data.remote.dto.MatchAdvanceSideBDto
import com.gentech.picklepro.data.remote.dto.MatchRemoteDto
import io.github.jan.supabase.functions.functions
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json

@Serializable
private data class IdOnly(val id: String)

/**
 * Flushes [com.gentech.picklepro.data.local.entity.PendingOpEntity] to
 * Supabase in creation order (spec §5.10). Conflict policy is
 * organizer-device-wins: every match sync is an unconditional `upsert`,
 * never a conditional merge. Stops at the first failure rather than
 * skipping ahead, so ordering (a match's own row before
 * `process-match-result` runs against it) is never violated.
 */
class SyncWorker(appContext: Context, params: WorkerParameters) : CoroutineWorker(appContext, params) {

    private val client = SupabaseModule.get(appContext)
    private val pendingOpDao = PickleProDatabase.get(appContext).pendingOpDao()
    private val json = Json { ignoreUnknownKeys = true }

    override suspend fun doWork(): Result {
        val ops = pendingOpDao.listAll()
        for (op in ops) {
            val outcome = runCatching { processOp(op) }
            if (outcome.isFailure) {
                pendingOpDao.incrementAttempts(op.id)
                return Result.retry()
            }
            pendingOpDao.remove(op.id)
        }
        return Result.success()
    }

    private suspend fun processOp(op: PendingOpEntity) {
        when (op.opType) {
            SyncOpType.UPSERT_MATCH -> {
                val match = json.decodeFromString<MatchRemoteDto>(op.payloadJson)
                client.postgrest.from("matches").upsert(match)
            }
            SyncOpType.PROCESS_MATCH_RESULT -> {
                val payload = json.decodeFromString<Map<String, String>>(op.payloadJson)
                val matchId = payload["matchId"] ?: return
                client.functions.invoke("process-match-result", body = mapOf("match_id" to matchId))
            }
            SyncOpType.ADVANCE_BRACKET -> advanceBracket(json.decodeFromString(op.payloadJson))
        }
    }

    /**
     * Round R position P feeds round R+1 position P/2, side A if P even
     * else B (see [AdvanceBracketPayload]). No match at that slot means
     * this was the final — nothing to advance into, not an error.
     */
    private suspend fun advanceBracket(payload: AdvanceBracketPayload) {
        val targetRound = payload.round + 1
        val targetPosition = payload.position / 2
        val isSideA = payload.position % 2 == 0

        val target = client.postgrest.from("matches")
            .select(Columns.list("id")) {
                filter {
                    eq("division_id", payload.divisionId)
                    eq("round", targetRound)
                    eq("position", targetPosition)
                }
            }
            .decodeList<IdOnly>()
            .firstOrNull() ?: return

        if (isSideA) {
            client.postgrest.from("matches")
                .update(MatchAdvanceSideADto(payload.winnerRef)) { filter { eq("id", target.id) } }
        } else {
            client.postgrest.from("matches")
                .update(MatchAdvanceSideBDto(payload.winnerRef)) { filter { eq("id", target.id) } }
        }
    }

    companion object {
        const val UNIQUE_WORK_NAME = "picklepro-sync"
    }
}
