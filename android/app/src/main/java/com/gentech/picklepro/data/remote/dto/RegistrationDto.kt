package com.gentech.picklepro.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Maps to public.registrations (spec §7). */
@Serializable
data class RegistrationDto(
    val id: String,
    @SerialName("division_id") val divisionId: String,
    @SerialName("player_id") val playerId: String,
    @SerialName("team_id") val teamId: String? = null,
    @SerialName("checked_in") val checkedIn: Boolean = false,
    @SerialName("effective_rating_at_reg") val effectiveRatingAtReg: Double? = null,
    @SerialName("created_at") val createdAt: String = "",
)

@Serializable
data class RegistrationInsertDto(
    @SerialName("division_id") val divisionId: String,
    @SerialName("player_id") val playerId: String,
    @SerialName("effective_rating_at_reg") val effectiveRatingAtReg: Double,
)

@Serializable
data class RegistrationCheckInUpdateDto(@SerialName("checked_in") val checkedIn: Boolean)

/** A registration with its player's public profile embedded, for the registration list (spec §5.4). */
@Serializable
data class RegistrationWithProfileDto(
    val id: String,
    @SerialName("division_id") val divisionId: String,
    @SerialName("player_id") val playerId: String,
    @SerialName("team_id") val teamId: String? = null,
    @SerialName("checked_in") val checkedIn: Boolean = false,
    @SerialName("effective_rating_at_reg") val effectiveRatingAtReg: Double? = null,
    val profiles: ProfileNameDto,
)

@Serializable
data class ProfileNameDto(
    val name: String,
    @SerialName("short_code") val shortCode: String? = null,
    @SerialName("is_shell") val isShell: Boolean = false,
)

/** Maps to public.teams (spec §7). */
@Serializable
data class TeamDto(
    val id: String,
    @SerialName("division_id") val divisionId: String,
    @SerialName("player1_id") val player1Id: String,
    @SerialName("player2_id") val player2Id: String? = null,
)

@Serializable
data class TeamInsertDto(
    @SerialName("division_id") val divisionId: String,
    @SerialName("player1_id") val player1Id: String,
    @SerialName("player2_id") val player2Id: String,
    val seed: Int? = null,
)
