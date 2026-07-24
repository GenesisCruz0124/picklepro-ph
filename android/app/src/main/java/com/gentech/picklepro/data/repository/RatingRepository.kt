package com.gentech.picklepro.data.repository

import android.content.Context
import com.gentech.picklepro.data.local.PickleProDatabase
import com.gentech.picklepro.data.local.entity.RatingEntity
import com.gentech.picklepro.data.remote.SupabaseModule
import com.gentech.picklepro.data.remote.dto.RatingDto
import com.gentech.picklepro.data.remote.dto.RatingHistoryDto
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.coroutines.flow.Flow

/** Ratings read-through cache, one row per event type (spec §3.2). */
class RatingRepository(context: Context) {

    private val client = SupabaseModule.get(context)
    private val dao = PickleProDatabase.get(context).ratingDao()

    fun observeCached(playerId: String): Flow<List<RatingEntity>> = dao.observeForPlayer(playerId)

    suspend fun refresh(playerId: String): List<RatingDto> {
        val dtos = client.postgrest.from("ratings")
            .select(Columns.ALL) { filter { eq("player_id", playerId) } }
            .decodeList<RatingDto>()
        dao.upsertAll(dtos.map { it.toEntity() })
        return dtos
    }

    /** Chart data for the profile's rating history (spec §4.2, Vico line chart). */
    suspend fun fetchHistory(playerId: String, eventType: String): List<RatingHistoryDto> =
        client.postgrest.from("rating_history")
            .select(Columns.ALL) {
                filter {
                    eq("player_id", playerId)
                    eq("event_type", eventType)
                }
                order("created_at", Order.ASCENDING)
            }
            .decodeList<RatingHistoryDto>()
}

private fun RatingDto.toEntity() = RatingEntity(
    playerId = playerId,
    eventType = eventType,
    elo = elo,
    display = display,
    matchesPlayed = matchesPlayed,
    provisional = provisional,
    override = override,
)
