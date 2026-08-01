package com.gentech.picklepro.organizer.dashboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.gentech.picklepro.R
import com.gentech.picklepro.data.remote.dto.TournamentDto
import com.gentech.picklepro.organizer.common.tournamentStatusLabelRes

@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel,
    onNewTournament: () -> Unit,
    onOpenTournament: (String) -> Unit,
    onOpenWallet: () -> Unit,
) {
    val state by viewModel.uiState.collectAsState()
    val pendingOpCount by viewModel.pendingOpCount.collectAsState()

    // Re-fetch whenever this screen comes back into view (e.g. after creating
    // a tournament and navigating back) — the ViewModel only loads once in
    // init, so without this, newly created tournaments stay invisible here
    // until the app process restarts.
    val currentViewModel by rememberUpdatedState(viewModel)
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) currentViewModel.refresh()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(stringResource(R.string.organizer_dashboard_title), style = MaterialTheme.typography.headlineMedium)
                TextButton(onClick = onOpenWallet) {
                    Text(stringResource(R.string.organizer_wallet_title))
                }
            }
        }

        if (pendingOpCount > 0) {
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        stringResource(R.string.sync_pending_ops, pendingOpCount),
                        modifier = Modifier.padding(12.dp),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                SummaryCard(
                    value = state.activeTournamentCount,
                    labelRes = R.string.organizer_dashboard_active_tournaments,
                    modifier = Modifier.weight(1f),
                )
                SummaryCard(
                    value = state.counts.totalRegisteredPlayers,
                    labelRes = R.string.organizer_dashboard_total_players,
                    modifier = Modifier.weight(1f),
                )
                SummaryCard(
                    value = state.counts.matchesPending + state.counts.matchesCompleted,
                    labelRes = R.string.organizer_dashboard_matches_today,
                    modifier = Modifier.weight(1f),
                )
            }
        }

        item {
            Button(onClick = onNewTournament, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.organizer_dashboard_new_tournament))
            }
        }

        if (state.isLoading) {
            item {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                    CircularProgressIndicator()
                }
            }
        } else if (state.tournaments.isEmpty()) {
            item {
                Text(
                    stringResource(R.string.organizer_dashboard_empty),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        } else {
            items(state.tournaments, key = { it.id }) { tournament ->
                TournamentRow(tournament, onClick = { onOpenTournament(tournament.id) })
            }
        }
    }
}

@Composable
private fun SummaryCard(value: Int, @androidx.annotation.StringRes labelRes: Int, modifier: Modifier = Modifier) {
    Card(modifier = modifier) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(text = value.toString(), style = MaterialTheme.typography.headlineLarge)
            Text(
                text = stringResource(labelRes),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun TournamentRow(tournament: TournamentDto, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column {
                Text(text = tournament.name, style = MaterialTheme.typography.titleLarge)
                tournament.venue?.takeIf { it.isNotBlank() }?.let {
                    Text(text = it, style = MaterialTheme.typography.bodyMedium)
                }
            }
            AssistChip(onClick = {}, label = { Text(stringResource(tournamentStatusLabelRes(tournament.status))) })
        }
    }
}
