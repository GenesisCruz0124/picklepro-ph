package com.gentech.picklepro.core.scoring

enum class Team { A, B }

enum class ScoringMode { SIDEOUT, RALLY }

enum class CourtSide { LEFT, RIGHT }

/** A single point-by-point log entry (spec §5.6: "feeds disputes + future sandbag analytics"). */
sealed interface ScoreEvent {
    /** [team] is whoever WON the rally — the engine derives scoring/rotation from game state, not the tap target. */
    data class RallyWon(val team: Team) : ScoreEvent
    data class Timeout(val team: Team) : ScoreEvent
}

data class GameState(
    val scoreA: Int = 0,
    val scoreB: Int = 0,
    /** Game starts 0-0-2 (spec §5.6): the opening server is nominally "server 2". */
    val servingTeam: Team = Team.A,
    val serverNumber: Int = 2,
    val timeoutsA: Int = 0,
    val timeoutsB: Int = 0,
)

/**
 * Event-sourced scoring engine (spec §5.6). [GameState] is never mutated
 * directly — it's always derived by [replay]ing the point log from empty,
 * which is what makes UNDO trivial and correct: drop the last event, replay
 * the rest. No separate "undo stack" is needed because the log already is
 * the single source of truth.
 */
object ScoreEngine {

    fun replay(events: List<ScoreEvent>, mode: ScoringMode, isDoubles: Boolean): GameState {
        var state = GameState()
        for (event in events) {
            state = when (event) {
                is ScoreEvent.RallyWon -> applyRally(state, event.team, mode, isDoubles)
                is ScoreEvent.Timeout -> applyTimeout(state, event.team)
            }
        }
        return state
    }

    private fun applyRally(state: GameState, rallyWinner: Team, mode: ScoringMode, isDoubles: Boolean): GameState =
        when {
            mode == ScoringMode.RALLY -> state.copy(
                scoreA = state.scoreA + if (rallyWinner == Team.A) 1 else 0,
                scoreB = state.scoreB + if (rallyWinner == Team.B) 1 else 0,
                servingTeam = rallyWinner,
                serverNumber = 1,
            )
            // Serving team wins the rally: they score and keep serving.
            rallyWinner == state.servingTeam -> state.copy(
                scoreA = state.scoreA + if (state.servingTeam == Team.A) 1 else 0,
                scoreB = state.scoreB + if (state.servingTeam == Team.B) 1 else 0,
            )
            // Receiving team wins: singles has no server 1/2, so it's always an immediate side-out.
            !isDoubles -> state.copy(servingTeam = rallyWinner, serverNumber = 1)
            // Doubles: server 1 losing serve passes to server 2 on the *same* team — not a side-out yet.
            state.serverNumber == 1 -> state.copy(serverNumber = 2)
            // Server 2 also lost serve: side-out to the other team, which always starts at server 1.
            else -> state.copy(servingTeam = rallyWinner, serverNumber = 1)
        }

    private fun applyTimeout(state: GameState, team: Team): GameState = when (team) {
        Team.A -> state.copy(timeoutsA = state.timeoutsA + 1)
        Team.B -> state.copy(timeoutsB = state.timeoutsB + 1)
    }

    /** Traditional scoring's score call: 3-number for doubles, 2-number for singles (spec §5.6). */
    fun scoreCall(state: GameState, mode: ScoringMode, isDoubles: Boolean): String {
        if (mode == ScoringMode.RALLY) return "${state.scoreA}-${state.scoreB}"
        val (servingScore, receivingScore) = if (state.servingTeam == Team.A) {
            state.scoreA to state.scoreB
        } else {
            state.scoreB to state.scoreA
        }
        return if (isDoubles) "$servingScore-$receivingScore-${state.serverNumber}" else "$servingScore-$receivingScore"
    }

    /** Right/even, left/odd — spec §5.6 court-position hint (shown as a text hint, not a diagram). */
    fun serveSideHint(state: GameState): CourtSide {
        val servingScore = if (state.servingTeam == Team.A) state.scoreA else state.scoreB
        return if (servingScore % 2 == 0) CourtSide.RIGHT else CourtSide.LEFT
    }

    fun gameWinner(state: GameState, gameTo: Int, winBy2: Boolean): Team? {
        val leadA = state.scoreA - state.scoreB
        return when {
            state.scoreA >= gameTo && (!winBy2 || leadA >= 2) -> Team.A
            state.scoreB >= gameTo && (!winBy2 || -leadA >= 2) -> Team.B
            else -> null
        }
    }

    fun matchWinner(gameWinners: List<Team>, bestOf: Int): Team? {
        val need = bestOf / 2 + 1
        return when {
            gameWinners.count { it == Team.A } >= need -> Team.A
            gameWinners.count { it == Team.B } >= need -> Team.B
            else -> null
        }
    }

    /**
     * True once [team] is exactly one point from winning this game. In
     * side-out mode this is only meaningful for the currently serving team
     * (only they can score on the next rally) — the caller decides which
     * team(s) to check based on [mode].
     */
    fun isGamePoint(state: GameState, team: Team, gameTo: Int, winBy2: Boolean): Boolean {
        val hypothetical = if (team == Team.A) {
            state.copy(scoreA = state.scoreA + 1)
        } else {
            state.copy(scoreB = state.scoreB + 1)
        }
        return gameWinner(hypothetical, gameTo, winBy2) == team
    }

    /** A game point that would also clinch the match. */
    fun isMatchPoint(
        state: GameState,
        team: Team,
        gameTo: Int,
        winBy2: Boolean,
        gameWinners: List<Team>,
        bestOf: Int,
    ): Boolean {
        if (!isGamePoint(state, team, gameTo, winBy2)) return false
        return matchWinner(gameWinners + team, bestOf) == team
    }

    /** spec §5.6: reminder to swap ends once either score first reaches 6 — game-to-11 only. */
    fun shouldShowEndSwapReminder(state: GameState, gameTo: Int): Boolean =
        gameTo == 11 && (state.scoreA == 6 || state.scoreB == 6)
}
