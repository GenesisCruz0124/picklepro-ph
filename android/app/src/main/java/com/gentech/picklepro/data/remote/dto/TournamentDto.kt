package com.gentech.picklepro.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Maps to public.tournaments (spec §7). */
@Serializable
data class TournamentDto(
    val id: String,
    @SerialName("organizer_id") val organizerId: String,
    val name: String,
    val venue: String? = null,
    val description: String? = null,
    @SerialName("logo_url") val logoUrl: String? = null,
    @SerialName("entry_fee_note") val entryFeeNote: String? = null,
    @SerialName("court_count") val courtCount: Int = 1,
    @SerialName("start_date") val startDate: String? = null,
    @SerialName("end_date") val endDate: String? = null,
    val status: String = "draft",
    @SerialName("created_at") val createdAt: String,
)

/** Partial update payload for a tournament's own fields (status excluded, see [TournamentStatusUpdateDto]). */
@Serializable
data class TournamentUpdateDto(
    val name: String? = null,
    val venue: String? = null,
    val description: String? = null,
    @SerialName("logo_url") val logoUrl: String? = null,
    @SerialName("entry_fee_note") val entryFeeNote: String? = null,
    @SerialName("court_count") val courtCount: Int? = null,
    @SerialName("start_date") val startDate: String? = null,
    @SerialName("end_date") val endDate: String? = null,
)

@Serializable
data class TournamentStatusUpdateDto(val status: String)

/** Body for the consume-code-on-create Edge Function (spec §5.1, §5.3). */
@Serializable
data class CreateTournamentRequest(
    val name: String,
    val venue: String? = null,
    val description: String? = null,
    @SerialName("entry_fee_note") val entryFeeNote: String? = null,
    @SerialName("logo_url") val logoUrl: String? = null,
    @SerialName("court_count") val courtCount: Int = 1,
    @SerialName("start_date") val startDate: String? = null,
    @SerialName("end_date") val endDate: String? = null,
)

@Serializable
data class CreateTournamentResponse(
    val ok: Boolean,
    @SerialName("tournament_id") val tournamentId: String? = null,
    @SerialName("credits_left") val creditsLeft: Int? = null,
    val error: String? = null,
)
