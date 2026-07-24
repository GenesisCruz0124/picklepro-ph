package com.gentech.picklepro.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Maps to public.ratings (spec §7); one row per player per event type. */
@Serializable
data class RatingDto(
    @SerialName("player_id") val playerId: String,
    @SerialName("event_type") val eventType: String,
    val elo: Int,
    val display: Double,
    @SerialName("matches_played") val matchesPlayed: Int,
    val provisional: Boolean,
    val override: Double? = null,
) {
    /** Admin override always wins for display/gating purposes (spec §3.4). */
    val effectiveDisplay: Double get() = override ?: display
}

/** Maps to public.rating_history (spec §7); one row per Elo change. */
@Serializable
data class RatingHistoryDto(
    val id: String,
    @SerialName("player_id") val playerId: String,
    @SerialName("event_type") val eventType: String,
    @SerialName("elo_before") val eloBefore: Int,
    @SerialName("elo_after") val eloAfter: Int,
    @SerialName("match_id") val matchId: String? = null,
    @SerialName("created_at") val createdAt: String,
)
