package com.gentech.picklepro.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Maps to public.divisions (spec §7). */
@Serializable
data class DivisionDto(
    val id: String,
    @SerialName("tournament_id") val tournamentId: String,
    @SerialName("event_type") val eventType: String,
    val name: String,
    @SerialName("min_rating") val minRating: Double? = null,
    @SerialName("max_rating") val maxRating: Double? = null,
    @SerialName("age_bracket") val ageBracket: String? = null,
    @SerialName("max_slots") val maxSlots: Int? = null,
    val format: String = "single_elim",
    @SerialName("game_to") val gameTo: Int = 11,
    @SerialName("win_by_2") val winBy2: Boolean = true,
    @SerialName("best_of") val bestOf: Int = 1,
    @SerialName("scoring_mode") val scoringMode: String = "sideout",
    @SerialName("bronze_match") val bronzeMatch: Boolean = false,
    val locked: Boolean = false,
    val published: Boolean = false,
)

/** Insert/update payload — [DivisionDto]'s server-owned fields (id, locked, published) are omitted. */
@Serializable
data class DivisionUpsertDto(
    @SerialName("tournament_id") val tournamentId: String,
    @SerialName("event_type") val eventType: String,
    val name: String,
    @SerialName("min_rating") val minRating: Double? = null,
    @SerialName("max_rating") val maxRating: Double? = null,
    @SerialName("age_bracket") val ageBracket: String? = null,
    @SerialName("max_slots") val maxSlots: Int? = null,
    val format: String = "single_elim",
    @SerialName("game_to") val gameTo: Int = 11,
    @SerialName("win_by_2") val winBy2: Boolean = true,
    @SerialName("best_of") val bestOf: Int = 1,
    @SerialName("scoring_mode") val scoringMode: String = "sideout",
    @SerialName("bronze_match") val bronzeMatch: Boolean = false,
)

@Serializable
data class DivisionLockUpdateDto(val locked: Boolean)

/** Results publish toggle (spec §5.8) — single-field on purpose, see TournamentLogoUpdateDto's note. */
@Serializable
data class DivisionPublishUpdateDto(val published: Boolean)
