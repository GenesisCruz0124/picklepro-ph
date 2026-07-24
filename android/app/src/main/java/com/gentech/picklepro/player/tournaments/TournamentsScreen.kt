package com.gentech.picklepro.player.tournaments

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
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.gentech.picklepro.R
import com.gentech.picklepro.data.remote.dto.TournamentDto
import com.gentech.picklepro.organizer.common.tournamentStatusLabelRes

// Section order: Upcoming (open registration) first, then Ongoing, then Finished.
private val SECTION_ORDER = listOf("registration", "ongoing", "finished")

@Composable
fun TournamentsScreen(
    viewModel: TournamentsViewModel,
    onOpenTournament: (tournamentId: String) -> Unit,
) {
    val state by viewModel.uiState.collectAsState()
    val grouped = state.byStatus

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Text(stringResource(R.string.tournaments_title), style = MaterialTheme.typography.headlineMedium)
        }
        item {
            OutlinedTextField(
                value = state.searchQuery,
                onValueChange = viewModel::onSearchChange,
                label = { Text(stringResource(R.string.tournaments_search_hint)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )
        }

        if (state.isLoading) {
            item { CircularProgressIndicator() }
        } else if (grouped.values.all { it.isEmpty() }) {
            item { Text(stringResource(R.string.tournaments_empty), style = MaterialTheme.typography.bodyMedium) }
        } else {
            SECTION_ORDER.forEach { status ->
                val list = grouped[status].orEmpty()
                if (list.isNotEmpty()) {
                    item {
                        Text(
                            stringResource(tournamentStatusLabelRes(status)),
                            style = MaterialTheme.typography.titleLarge,
                        )
                    }
                    items(list, key = { it.id }) { tournament ->
                        TournamentCard(tournament, onClick = { onOpenTournament(tournament.id) })
                    }
                }
            }
        }
    }
}

@Composable
private fun TournamentCard(tournament: TournamentDto, onClick: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth(), onClick = onClick) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(tournament.name, style = MaterialTheme.typography.titleLarge)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                tournament.venue?.takeIf { it.isNotBlank() }?.let {
                    Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                tournament.startDate?.let {
                    Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}
