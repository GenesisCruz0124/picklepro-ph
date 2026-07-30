package com.gentech.picklepro.player.tournaments

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.gentech.picklepro.R
import com.gentech.picklepro.core.designsystem.PickleProTopBar
import com.gentech.picklepro.data.remote.dto.DivisionDto
import com.gentech.picklepro.organizer.common.tournamentStatusLabelRes

@Composable
fun PlayerTournamentDetailScreen(viewModel: PlayerTournamentDetailViewModel, onBack: () -> Unit) {
    val state by viewModel.uiState.collectAsState()
    val tournament = state.tournament

    Scaffold(
        topBar = { PickleProTopBar(tournament?.name ?: stringResource(R.string.tournaments_title), onBack) },
    ) { padding ->
        if (state.isLoading || tournament == null) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Text(stringResource(tournamentStatusLabelRes(tournament.status)), style = MaterialTheme.typography.labelLarge)
            }
            tournament.venue?.takeIf { it.isNotBlank() }?.let {
                item { Text(it, style = MaterialTheme.typography.bodyLarge) }
            }
            item {
                val dates = listOfNotNull(tournament.startDate, tournament.endDate).distinct().joinToString(" – ")
                if (dates.isNotBlank()) Text(dates, style = MaterialTheme.typography.bodyMedium)
            }
            tournament.entryFeeNote?.takeIf { it.isNotBlank() }?.let {
                item { Text(it, style = MaterialTheme.typography.bodyMedium) }
            }
            if (state.organizerName.isNotBlank()) {
                item {
                    Text(
                        "${stringResource(R.string.player_tournament_organizer_label)}: ${state.organizerName}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            tournament.description?.takeIf { it.isNotBlank() }?.let {
                item { Text(it, style = MaterialTheme.typography.bodyMedium) }
            }

            item {
                Text(stringResource(R.string.player_tournament_divisions_title), style = MaterialTheme.typography.titleLarge)
            }
            items(state.divisions, key = { it.id }) { division ->
                DivisionCard(
                    division = division,
                    registeredCount = state.registeredCountByDivision[division.id] ?: 0,
                    published = state.resultsByDivision[division.id],
                )
            }
        }
    }
}

@Composable
private fun DivisionCard(division: DivisionDto, registeredCount: Int, published: PublishedResults?) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(division.name, style = MaterialTheme.typography.titleLarge)
            val gate = division.maxRating?.let { "≤ $it" } ?: stringResource(R.string.division_skill_gate_open)
            val age = division.ageBracket ?: stringResource(R.string.division_age_bracket_open)
            Text(
                "$gate • $age",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            val slotsLabel = division.maxSlots?.let { "$registeredCount / $it" } ?: registeredCount.toString()
            Text(
                stringResource(R.string.player_division_registered, slotsLabel),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            published?.let { podium ->
                Text(stringResource(R.string.results_title), style = MaterialTheme.typography.titleMedium)
                podium.results.championRef?.let {
                    Text("🏆 ${podium.nameByRef[it] ?: "?"}", style = MaterialTheme.typography.bodyLarge)
                }
                podium.results.runnerUpRef?.let {
                    Text("🥈 ${podium.nameByRef[it] ?: "?"}", style = MaterialTheme.typography.bodyMedium)
                }
                podium.results.thirdPlaceRef?.let {
                    Text("🥉 ${podium.nameByRef[it] ?: "?"}", style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }
}
