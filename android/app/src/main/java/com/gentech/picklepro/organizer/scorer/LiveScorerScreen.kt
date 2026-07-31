package com.gentech.picklepro.organizer.scorer

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.gentech.picklepro.R
import com.gentech.picklepro.core.scoring.CourtSide
import com.gentech.picklepro.core.scoring.ScoreEngine
import com.gentech.picklepro.core.scoring.ScoringMode
import com.gentech.picklepro.core.scoring.Team
import com.gentech.picklepro.data.local.decodeEventLog
import kotlinx.coroutines.delay

@Composable
fun LiveScorerScreen(
    viewModel: LiveScorerViewModel,
    onOpenScoreboard: () -> Unit,
    onOpenBracket: () -> Unit,
    onBack: () -> Unit,
) {
    val state by viewModel.uiState.collectAsState()
    val match = state.match
    val config = viewModel.config

    if (state.isLoading || match == null || config == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    // ScoreEngine.replay is pure/cheap; re-derive straight from the observed match on every recomposition.
    val fullLog = decodeEventLog(match.pointLogJson)
    val gameState = ScoreEngine.replay(
        fullLog.filter { it.first == match.currentGameNumber }.map { it.second },
        config.mode,
        config.isDoubles,
    )
    // Winners of already-completed games this match, needed to tell a game point
    // that would also clinch the match ("Match Point") from an ordinary one.
    val gameWinners = (1 until match.currentGameNumber).mapNotNull { gameNumber ->
        val finalState = ScoreEngine.replay(
            fullLog.filter { it.first == gameNumber }.map { it.second },
            config.mode,
            config.isDoubles,
        )
        ScoreEngine.gameWinner(finalState, config.gameTo, config.winBy2)
    }

    var timeoutTeam by remember { mutableStateOf<Team?>(null) }
    var timeoutSecondsLeft by remember { mutableStateOf(0) }
    LaunchedEffect(timeoutTeam) {
        if (timeoutTeam == null) return@LaunchedEffect
        timeoutSecondsLeft = 60
        while (timeoutSecondsLeft > 0) {
            delay(1000)
            timeoutSecondsLeft -= 1
        }
        timeoutTeam = null
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        val displayGameNumber = if (match.status == "done") {
            match.currentGameNumber - 1
        } else {
            match.currentGameNumber
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.common_back))
            }
            Text(
                stringResource(R.string.scorer_game_label, displayGameNumber.coerceAtLeast(1)),
                style = MaterialTheme.typography.titleLarge,
            )
        }

        if (ScoreEngine.shouldShowEndSwapReminder(gameState, config.gameTo)) {
            Card(modifier = Modifier.fillMaxWidth()) {
                Text(
                    stringResource(R.string.scorer_end_swap_reminder),
                    modifier = Modifier.padding(12.dp),
                    style = MaterialTheme.typography.titleMedium,
                )
            }
        }

        Text(
            ScoreEngine.scoreCall(gameState, config.mode, config.isDoubles),
            style = MaterialTheme.typography.displayMedium,
        )

        Row(modifier = Modifier.fillMaxWidth().weight(1f), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            TeamPanel(
                name = state.nameA,
                score = gameState.scoreA,
                isServing = gameState.servingTeam == Team.A,
                isGamePoint = ScoreEngine.isGamePoint(gameState, Team.A, config.gameTo, config.winBy2),
                isMatchPoint = ScoreEngine.isMatchPoint(
                    gameState, Team.A, config.gameTo, config.winBy2, gameWinners, config.bestOf,
                ),
                canTapToScore = config.mode == ScoringMode.RALLY || gameState.servingTeam == Team.A,
                courtSide = if (gameState.servingTeam == Team.A) ScoreEngine.serveSideHint(gameState) else null,
                modifier = Modifier.weight(1f),
                onTap = {
                    if (config.mode == ScoringMode.RALLY) viewModel.onRallyPointTapped(Team.A) else viewModel.onServingPanelTapped()
                },
            )
            TeamPanel(
                name = state.nameB,
                score = gameState.scoreB,
                isServing = gameState.servingTeam == Team.B,
                isGamePoint = ScoreEngine.isGamePoint(gameState, Team.B, config.gameTo, config.winBy2),
                isMatchPoint = ScoreEngine.isMatchPoint(
                    gameState, Team.B, config.gameTo, config.winBy2, gameWinners, config.bestOf,
                ),
                canTapToScore = config.mode == ScoringMode.RALLY || gameState.servingTeam == Team.B,
                courtSide = if (gameState.servingTeam == Team.B) ScoreEngine.serveSideHint(gameState) else null,
                modifier = Modifier.weight(1f),
                onTap = {
                    if (config.mode == ScoringMode.RALLY) viewModel.onRallyPointTapped(Team.B) else viewModel.onServingPanelTapped()
                },
            )
        }

        if (config.mode == ScoringMode.SIDEOUT) {
            Button(onClick = viewModel::onSideOut, modifier = Modifier.fillMaxWidth().height(56.dp)) {
                Text(stringResource(R.string.scorer_side_out_button))
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            OutlinedButton(onClick = viewModel::onUndo, modifier = Modifier.weight(1f)) {
                Text(stringResource(R.string.scorer_undo_button))
            }
            OutlinedButton(
                onClick = { timeoutTeam = Team.A; viewModel.onTimeout(Team.A) },
                enabled = gameState.timeoutsA < 2,
                modifier = Modifier.weight(1f),
            ) {
                Text("${stringResource(R.string.scorer_timeout_button)} A (${2 - gameState.timeoutsA})")
            }
            OutlinedButton(
                onClick = { timeoutTeam = Team.B; viewModel.onTimeout(Team.B) },
                enabled = gameState.timeoutsB < 2,
                modifier = Modifier.weight(1f),
            ) {
                Text("${stringResource(R.string.scorer_timeout_button)} B (${2 - gameState.timeoutsB})")
            }
        }

        if (timeoutTeam != null) {
            Text(
                stringResource(R.string.scorer_timeout_countdown, timeoutSecondsLeft),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.error,
            )
        }

        TextButton(onClick = onOpenScoreboard, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.scorer_open_scoreboard_button))
        }
    }

    if (state.showCompleteDialog) {
        val winnerName = if (match.winnerRef == match.sideARef) state.nameA else state.nameB
        AlertDialog(
            onDismissRequest = viewModel::dismissCompleteDialog,
            title = { Text(stringResource(R.string.scorer_match_complete_title)) },
            text = { Text(stringResource(R.string.scorer_match_complete_message, winnerName)) },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.dismissCompleteDialog()
                    onOpenBracket()
                }) { Text(stringResource(R.string.bracket_view_button)) }
            },
            dismissButton = {
                TextButton(onClick = viewModel::dismissCompleteDialog) { Text(stringResource(R.string.scorer_ok_button)) }
            },
        )
    }
}

@Composable
private fun TeamPanel(
    name: String,
    score: Int,
    isServing: Boolean,
    isGamePoint: Boolean,
    isMatchPoint: Boolean,
    canTapToScore: Boolean,
    courtSide: CourtSide?,
    onTap: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxSize(),
        onClick = { if (canTapToScore) onTap() },
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            if (isServing) Text("●", color = MaterialTheme.colorScheme.primary)
            Text(name, style = MaterialTheme.typography.titleLarge)
            Text(score.toString(), style = MaterialTheme.typography.displayLarge)
            if (isMatchPoint) {
                Text(stringResource(R.string.scorer_match_point), color = MaterialTheme.colorScheme.error)
            } else if (isGamePoint) {
                Text(stringResource(R.string.scorer_game_point), color = MaterialTheme.colorScheme.error)
            }
            courtSide?.let {
                Text(
                    stringResource(if (it == CourtSide.RIGHT) R.string.scorer_court_right else R.string.scorer_court_left),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
    }
}
