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
fun BracketViewScreen(viewModel: BracketViewViewModel) {
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
            Text(
                stringResource(if (isRoundRobin) R.string.bracket_standings_title else R.string.bracket_view_title),
                style = MaterialTheme.typography.headlineMedium,
            )
        }

        if (isRoundRobin) {
            items(state.standings, key = { it.entrantRef }) { row ->
                StandingsRowView(row)
            }
        } else {
            state.matchesByRound.toSortedMap().forEach { (round, matches) ->
                item {
                    Text(
                        stringResource(R.string.bracket_round_label, round),
                        style = MaterialTheme.typography.titleLarge,
                    )
                }
                items(matches.sortedBy { it.position }, key = { it.id }) { match ->
                    BracketMatchRow(match, state.nameByRef)
                }
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
private fun BracketMatchRow(match: BracketMatchDto, nameByRef: Map<String, String>) {
    val nameA = match.sideARef?.let { nameByRef[it] } ?: stringResource(R.string.bracket_bye_label)
    val nameB = match.sideBRef?.let { nameByRef[it] } ?: stringResource(R.string.bracket_bye_label)
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text("$nameA  vs  $nameB", style = MaterialTheme.typography.titleLarge)
            Text(match.status, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
