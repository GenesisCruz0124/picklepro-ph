package com.gentech.picklepro.organizer.scoreboard

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.ActivityInfo
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.gentech.picklepro.R
import com.gentech.picklepro.core.scoring.ScoreEngine
import com.gentech.picklepro.core.scoring.ScoringMode
import com.gentech.picklepro.core.scoring.Team
import com.gentech.picklepro.data.local.decodeEventLog

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

@Composable
fun ScoreboardScreen(viewModel: ScoreboardViewModel) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val activity = remember(context) { context.findActivity() }
    val view = LocalView.current

    DisposableEffect(activity) {
        view.keepScreenOn = true
        val originalOrientation = activity?.requestedOrientation
        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        onDispose {
            view.keepScreenOn = false
            if (originalOrientation != null) activity.requestedOrientation = originalOrientation
        }
    }

    val match = state.match
    val division = state.division
    if (state.isLoading || match == null || division == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    val mode = if (division.scoringMode == "rally") ScoringMode.RALLY else ScoringMode.SIDEOUT
    val isDoubles = division.eventType != "singles"
    val gameState = ScoreEngine.replay(
        decodeEventLog(match.pointLogJson).filter { it.first == match.currentGameNumber }.map { it.second },
        mode,
        isDoubles,
    )
    val displayGameNumber = (if (match.status == "done") match.currentGameNumber - 1 else match.currentGameNumber)
        .coerceAtLeast(1)

    Column(modifier = Modifier.fillMaxSize().padding(24.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column {
                Text(state.tournamentName, style = MaterialTheme.typography.headlineMedium)
                Text(division.name, style = MaterialTheme.typography.titleLarge)
            }
            state.organizerLogoUrl?.let {
                AsyncImage(model = it, contentDescription = null, modifier = Modifier.size(56.dp))
            }
            Text(
                stringResource(R.string.scorer_game_label, displayGameNumber),
                style = MaterialTheme.typography.titleLarge,
            )
        }

        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.spacedBy(24.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ScoreboardSide(
                name = state.nameA,
                score = gameState.scoreA,
                isServing = gameState.servingTeam == Team.A,
                serverNumber = if (mode == ScoringMode.SIDEOUT && isDoubles) gameState.serverNumber else null,
                modifier = Modifier.weight(1f),
            )
            Text("–", style = MaterialTheme.typography.displayLarge)
            ScoreboardSide(
                name = state.nameB,
                score = gameState.scoreB,
                isServing = gameState.servingTeam == Team.B,
                serverNumber = if (mode == ScoringMode.SIDEOUT && isDoubles) gameState.serverNumber else null,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun ScoreboardSide(
    name: String,
    score: Int,
    isServing: Boolean,
    serverNumber: Int?,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(name, style = MaterialTheme.typography.headlineMedium)
        Text(score.toString(), style = MaterialTheme.typography.displayLarge)
        if (isServing) {
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                if (serverNumber != null) {
                    repeat(2) { index ->
                        val dotFilled = index == serverNumber - 1
                        Text(if (dotFilled) "●" else "○", color = MaterialTheme.colorScheme.primary)
                    }
                } else {
                    Text("●", color = MaterialTheme.colorScheme.primary)
                }
            }
        }
    }
}
