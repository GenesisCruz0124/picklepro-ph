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
)

/** Maps to public.teams (spec §7). */
@Serializable
data class TeamDto(
    val id: String,
    @SerialName("division_id") val divisionId: String,
    @SerialName("player1_id") val player1Id: String,
    @SerialName("player2_id") val player2Id: String? = null,
)
