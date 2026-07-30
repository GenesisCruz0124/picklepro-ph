package com.gentech.picklepro.organizer.results

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
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.gentech.picklepro.R
import com.gentech.picklepro.data.repository.StandingsRow

@Composable
fun ResultsScreen(
    viewModel: ResultsViewModel,
    onOpenCertificates: () -> Unit,
) {
    val state by viewModel.uiState.collectAsState()
    val division = state.division
    val results = state.results

    if (state.isLoading || division == null || results == null) {
        Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center) {
            CircularProgressIndicator(modifier = Modifier.padding(24.dp))
        }
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Text(
                "${stringResource(R.string.results_title)} — ${division.name}",
                style = MaterialTheme.typography.headlineMedium,
            )
        }

        state.errorMessage?.let { message ->
            item { Text(message, color = MaterialTheme.colorScheme.error) }
        }

        if (results.standings.isEmpty() && results.championRef == null) {
            item { Text(stringResource(R.string.results_no_matches), style = MaterialTheme.typography.bodyMedium) }
        } else {
            if (!results.isComplete) {
                item {
                    Text(
                        stringResource(R.string.results_incomplete_note),
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }

            results.championRef?.let { ref ->
                item { PodiumCard(R.string.results_champion, state.nameByRef[ref] ?: "?") }
            }
            results.runnerUpRef?.let { ref ->
                item { PodiumCard(R.string.results_runner_up, state.nameByRef[ref] ?: "?") }
            }
            results.thirdPlaceRef?.let { ref ->
                item { PodiumCard(R.string.results_third_place, state.nameByRef[ref] ?: "?") }
            }

            item {
                Text(stringResource(R.string.bracket_standings_title), style = MaterialTheme.typography.titleLarge)
            }
            items(results.standings, key = { it.entrantRef }) { row ->
                StandingsLine(row)
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(stringResource(R.string.results_publish_label), style = MaterialTheme.typography.titleLarge)
                    Text(
                        stringResource(R.string.results_publish_hint),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Switch(
                    checked = division.published,
                    onCheckedChange = viewModel::setPublished,
                    enabled = !state.isTogglingPublish,
                )
            }
        }

        item {
            OutlinedButton(onClick = onOpenCertificates, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.results_certificates_button))
            }
        }
    }
}

@Composable
private fun PodiumCard(@androidx.annotation.StringRes labelRes: Int, name: String) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                stringResource(labelRes),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
            )
            Text(name, style = MaterialTheme.typography.headlineMedium)
        }
    }
}

@Composable
private fun StandingsLine(row: StandingsRow) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(row.name, style = MaterialTheme.typography.bodyLarge)
        Text(
            "${stringResource(R.string.bracket_standings_wins)} ${row.wins} · " +
                "${stringResource(R.string.bracket_standings_losses)} ${row.losses} · " +
                "${stringResource(R.string.bracket_standings_point_diff)} ${row.pointDiff}",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
