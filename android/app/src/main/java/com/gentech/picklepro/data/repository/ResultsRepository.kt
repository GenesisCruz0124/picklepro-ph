package com.gentech.picklepro.data.repository

import android.content.Context
import com.gentech.picklepro.data.remote.dto.DivisionDto

/** Auto-derived podium for a division (spec §5.8) — refs are registration/team ids. */
data class DivisionResults(
    /** True once every generated match is done/walkover — placings are only final then. */
    val isComplete: Boolean,
    val championRef: String?,
    val runnerUpRef: String?,
    /** Only present when the division had a bronze match with a result. */
    val thirdPlaceRef: String?,
    val standings: List<StandingsRow>,
)

/**
 * Champions + runners-up are derived, never stored (spec §5.8): single elim
 * reads them off the final (max round, position 0 — the bronze match, when
 * present, sits at position 1 of the same round per BracketRepository's
 * generator); round robin takes the top of the tiebroken standings.
 */
class ResultsRepository(context: Context) {

    private val bracketRepository = BracketRepository(context)

    suspend fun deriveResults(division: DivisionDto, entrants: List<Entrant>): DivisionResults {
        val matches = bracketRepository.listMatches(division.id)
        val isComplete = matches.isNotEmpty() && matches.all { it.status == "done" || it.status == "walkover" }
        val standings = bracketRepository.computeStandings(division.id, entrants)

        if (division.format == "round_robin") {
            return DivisionResults(
                isComplete = isComplete,
                championRef = if (isComplete) standings.getOrNull(0)?.entrantRef else null,
                runnerUpRef = if (isComplete) standings.getOrNull(1)?.entrantRef else null,
                thirdPlaceRef = null,
                standings = standings,
            )
        }

        val finalRound = matches.maxOfOrNull { it.round } ?: return DivisionResults(false, null, null, null, standings)
        val finalMatch = matches.firstOrNull { it.round == finalRound && it.position == 0 }
        val bronzeMatch = matches.firstOrNull { it.round == finalRound && it.position == 1 }

        val championRef = finalMatch?.winnerRef
        val runnerUpRef = finalMatch?.let { f ->
            f.winnerRef?.let { winner -> if (winner == f.sideARef) f.sideBRef else f.sideARef }
        }
        return DivisionResults(
            isComplete = isComplete,
            championRef = championRef,
            runnerUpRef = runnerUpRef,
            thirdPlaceRef = bronzeMatch?.winnerRef,
            standings = standings,
        )
    }
}
