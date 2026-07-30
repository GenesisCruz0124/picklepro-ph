package com.gentech.picklepro.organizer.bracket

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.gentech.picklepro.R
import com.gentech.picklepro.data.remote.dto.BracketMatchDto
import com.gentech.picklepro.data.repository.StandingsRow

@Composable
fun BracketViewScreen(
    viewModel: BracketViewViewModel,
    onOpenMatch: (matchId: String) -> Unit,
) {
    val state by viewModel.uiState.collectAsState()

    if (state.isLoading) {
        Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center) {
            CircularProgressIndicator(modifier = Modifier.padding(24.dp))
        }
        return
    }

    val isRoundRobin = state.division?.format == "round_robin"

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Text(stringResource(R.string.bracket_view_title), style = MaterialTheme.typography.headlineMedium)
        }

        state.matchesByRound.toSortedMap().forEach { (round, matches) ->
            item {
                Text(
                    stringResource(R.string.bracket_round_label, round),
                    style = MaterialTheme.typography.titleLarge,
                )
            }
            items(matches.sortedBy { it.position }, key = { it.id }) { match ->
                BracketMatchRow(match, state.nameByRef, onClick = { onOpenMatch(match.id) })
            }
        }

        if (isRoundRobin) {
            item {
                Text(stringResource(R.string.bracket_standings_title), style = MaterialTheme.typography.headlineMedium)
            }
            items(state.standings, key = { it.entrantRef }) { row ->
                StandingsRowView(row)
            }
        }
    }
}

@Composable
private fun StandingsRowView(row: StandingsRow) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(row.name, style = MaterialTheme.typography.titleLarge)
            Text(
                "${stringResource(R.string.bracket_standings_wins)} ${row.wins} · " +
                    "${stringResource(R.string.bracket_standings_losses)} ${row.losses} · " +
                    "${stringResource(R.string.bracket_standings_point_diff)} ${row.pointDiff}",
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}

@Composable
private fun BracketMatchRow(match: BracketMatchDto, nameByRef: Map<String, String>, onClick: () -> Unit) {
    val bye = stringResource(R.string.bracket_bye_label)
    val nameA = match.sideARef?.let { nameByRef[it] } ?: bye
    val nameB = match.sideBRef?.let { nameByRef[it] } ?: bye
    val ready = match.sideARef != null && match.sideBRef != null

    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = if (ready) onClick else {},
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text("$nameA  vs  $nameB", style = MaterialTheme.typography.titleLarge)
            Text(
                if (ready) stringResource(matchStatusLabelRes(match.status)) else stringResource(R.string.bracket_match_not_ready),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

private fun matchStatusLabelRes(status: String): Int = when (status) {
    "live" -> R.string.match_status_live
    "done" -> R.string.match_status_done
    "walkover" -> R.string.match_status_walkover
    else -> R.string.match_status_pending
}
