package com.gentech.picklepro.data.repository

import android.content.Context
import com.gentech.picklepro.data.remote.SupabaseModule
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns
import kotlinx.serialization.Serializable

data class DashboardCounts(
    val totalRegisteredPlayers: Int,
    val matchesPending: Int,
    val matchesCompleted: Int,
)

@Serializable
private data class IdRow(val id: String)

@Serializable
private data class StatusRow(val status: String)

/**
 * Registration/match aggregate counts for the organizer dashboard (spec
 * §5.2). Resolved via the organizer's division ids rather than a deeply
 * embedded Postgrest filter, matching the same multi-step pattern used by
 * [MatchHistoryRepository]. Both counts are legitimately 0 until M4/M5 add
 * the registration and live-scorer UIs that write these rows.
 */
class DashboardRepository(context: Context) {

    private val client = SupabaseModule.get(context)

    suspend fun getCounts(tournamentIds: List<String>): DashboardCounts {
        if (tournamentIds.isEmpty()) return DashboardCounts(0, 0, 0)

        val divisionIds = client.postgrest.from("divisions")
            .select(Columns.list("id")) { filter { isIn("tournament_id", tournamentIds) } }
            .decodeList<IdRow>()
            .map { it.id }
        if (divisionIds.isEmpty()) return DashboardCounts(0, 0, 0)

        val registeredPlayers = client.postgrest.from("registrations")
            .select(Columns.list("id")) { filter { isIn("division_id", divisionIds) } }
            .decodeList<IdRow>()
            .size

        val matchStatuses = client.postgrest.from("matches")
            .select(Columns.list("status")) { filter { isIn("division_id", divisionIds) } }
            .decodeList<StatusRow>()

        return DashboardCounts(
            totalRegisteredPlayers = registeredPlayers,
            matchesPending = matchStatuses.count { it.status == "pending" || it.status == "live" },
            matchesCompleted = matchStatuses.count { it.status == "done" || it.status == "walkover" },
        )
    }
}
