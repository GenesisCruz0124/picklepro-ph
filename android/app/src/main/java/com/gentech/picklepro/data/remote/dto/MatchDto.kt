package com.gentech.picklepro.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** One entry in matches.games jsonb, e.g. [{"a":11,"b":7}, ...] (spec §7). */
@Serializable
data class GameScoreDto(val a: Int, val b: Int)

@Serializable
data class TournamentSummaryDto(val name: String)

@Serializable
data class DivisionSummaryDto(
    val name: String,
    @SerialName("event_type") val eventType: String,
    val tournaments: TournamentSummaryDto,
)

/**
 * A finished match embedding its division + tournament names, used to build
 * the player's match history list (spec §4.2). [sideARef]/[sideBRef]/
 * [winnerRef] point at either a registrations.id (singles) or teams.id
 * (doubles/mixed) — resolved to opponent names by the repository.
 */
@Serializable
data class MatchResultDto(
    val id: String,
    @SerialName("side_a_ref") val sideARef: String? = null,
    @SerialName("side_b_ref") val sideBRef: String? = null,
    @SerialName("winner_ref") val winnerRef: String? = null,
    val games: List<GameScoreDto> = emptyList(),
    @SerialName("finished_at") val finishedAt: String? = null,
    val divisions: DivisionSummaryDto,
)

/** A bracket/round-robin match, for generation and the bracket/standings view (spec §5.5). */
@Serializable
data class BracketMatchDto(
    val id: String,
    @SerialName("division_id") val divisionId: String,
    val round: Int,
    val position: Int,
    @SerialName("side_a_ref") val sideARef: String? = null,
    @SerialName("side_b_ref") val sideBRef: String? = null,
    val status: String = "pending",
    @SerialName("winner_ref") val winnerRef: String? = null,
    val games: List<GameScoreDto> = emptyList(),
)

@Serializable
data class MatchInsertDto(
    @SerialName("division_id") val divisionId: String,
    val round: Int,
    val position: Int,
    @SerialName("side_a_ref") val sideARef: String? = null,
    @SerialName("side_b_ref") val sideBRef: String? = null,
    val status: String = "pending",
)
