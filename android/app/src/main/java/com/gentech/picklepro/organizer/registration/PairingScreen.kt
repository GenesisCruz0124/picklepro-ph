package com.gentech.picklepro.organizer.registration

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.gentech.picklepro.R
import com.gentech.picklepro.data.remote.dto.RegistrationWithProfileDto

@Composable
fun PairingScreen(viewModel: PairingViewModel) {
    val state by viewModel.uiState.collectAsState()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Text(stringResource(R.string.pairing_title), style = MaterialTheme.typography.headlineMedium)
        }
        item {
            Text(stringResource(R.string.pairing_select_two), style = MaterialTheme.typography.bodyMedium)
        }
        state.errorMessage?.let { message ->
            item { Text(message, color = MaterialTheme.colorScheme.error) }
        }

        if (state.isLoading) {
            item { CircularProgressIndicator() }
        } else {
            item { Text(stringResource(R.string.pairing_unpaired_title), style = MaterialTheme.typography.titleLarge) }
            items(state.unpaired, key = { it.id }) { registration ->
                UnpairedRow(
                    registration = registration,
                    selected = registration.id in state.selectedIds,
                    onToggle = { viewModel.toggleSelect(registration.id) },
                )
            }
            item {
                Button(
                    onClick = viewModel::pairSelected,
                    enabled = state.selectedIds.size == 2,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(stringResource(R.string.pairing_pair_button))
                }
            }

            item { Text(stringResource(R.string.pairing_teams_title), style = MaterialTheme.typography.titleLarge) }
            items(state.teams, key = { it.first().teamId ?: it.first().id }) { team ->
                TeamRow(team = team, onUnpair = { viewModel.unpair(team.first().teamId!!) })
            }
        }
    }
}

@Composable
private fun UnpairedRow(registration: RegistrationWithProfileDto, selected: Boolean, onToggle: () -> Unit) {
    FilterChip(
        selected = selected,
        onClick = onToggle,
        label = { Text(registration.profiles.name) },
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun TeamRow(team: List<RegistrationWithProfileDto>, onUnpair: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(team.joinToString(" / ") { it.profiles.name })
            TextButton(onClick = onUnpair) { Text(stringResource(R.string.pairing_unpair_button)) }
        }
    }
}
