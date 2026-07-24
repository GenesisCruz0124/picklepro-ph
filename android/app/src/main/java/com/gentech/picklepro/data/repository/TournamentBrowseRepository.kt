package com.gentech.picklepro.data.repository

import android.content.Context
import com.gentech.picklepro.data.remote.SupabaseModule
import com.gentech.picklepro.data.remote.dto.TournamentDto
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
private data class DivisionIdRow(@SerialName("division_id") val divisionId: String)

/**
 * Player-side tournament browsing (spec §4.4). Reads only — RLS already
 * limits players to tournaments that have left draft, so the status filter
 * here just mirrors what the server enforces.
 */
class TournamentBrowseRepository(context: Context) {

    private val client = SupabaseModule.get(context)

    suspend fun listVisible(): List<TournamentDto> =
        client.postgrest.from("tournaments")
            .select(Columns.ALL) {
                filter { neq("status", "draft") }
                order("start_date", Order.ASCENDING, nullsFirst = false)
            }
            .decodeList()

    /** Registered-player counts per division, one query for the whole tournament. */
    suspend fun registrationCounts(divisionIds: List<String>): Map<String, Int> {
        if (divisionIds.isEmpty()) return emptyMap()
        return client.postgrest.from("registrations")
            .select(Columns.list("division_id")) { filter { isIn("division_id", divisionIds) } }
            .decodeList<DivisionIdRow>()
            .groupingBy { it.divisionId }
            .eachCount()
    }
}
