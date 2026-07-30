package com.gentech.picklepro.data.sync

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Bracket advancement (spec §5.6 "winner auto-advances in bracket") runs
 * server-side as a sync follow-up rather than against the local Room cache:
 * the *next* round's match may not be cached on this device at all (the
 * organizer hasn't opened it yet), so there's nothing reliable to advance
 * into locally. The arithmetic — round R position P feeds round R+1
 * position P/2, side A if P even else B — only holds because
 * [com.gentech.picklepro.data.repository.BracketRepository] numbers every
 * round-1 pairing (byes included), matching every later round.
 *
 * Scope note: only standard winner-advancement is handled. A division's
 * optional bronze match (loser-of-semifinal, not winner) is generated as a
 * slot in M4 but does not auto-populate — a known, documented gap.
 */
@Serializable
data class AdvanceBracketPayload(
    @SerialName("division_id") val divisionId: String,
    val round: Int,
    val position: Int,
    @SerialName("winner_ref") val winnerRef: String,
)
