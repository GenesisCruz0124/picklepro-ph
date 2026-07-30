package com.gentech.picklepro.organizer.tournament

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.gentech.picklepro.R

@Composable
fun NewTournamentScreen(
    viewModel: NewTournamentViewModel,
    onCreated: (tournamentId: String) -> Unit,
) {
    val state by viewModel.uiState.collectAsState()

    LaunchedEffect(state.createdTournamentId) {
        state.createdTournamentId?.let(onCreated)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(stringResource(R.string.tournament_setup_title_new), style = MaterialTheme.typography.headlineMedium)

        if (state.credits == 0) {
            Text(stringResource(R.string.tournament_no_credits), color = MaterialTheme.colorScheme.error)
        }

        OutlinedTextField(
            value = state.name,
            onValueChange = viewModel::onNameChange,
            label = { Text(stringResource(R.string.tournament_name_label)) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
        )
        OutlinedTextField(
            value = state.venue,
            onValueChange = viewModel::onVenueChange,
            label = { Text(stringResource(R.string.tournament_venue_label)) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
        )
        OutlinedTextField(
            value = state.description,
            onValueChange = viewModel::onDescriptionChange,
            label = { Text(stringResource(R.string.tournament_description_label)) },
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = state.entryFeeNote,
            onValueChange = viewModel::onEntryFeeNoteChange,
            label = { Text(stringResource(R.string.tournament_entry_fee_label)) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
        )
        OutlinedTextField(
            value = state.courtCount,
            onValueChange = viewModel::onCourtCountChange,
            label = { Text(stringResource(R.string.tournament_court_count_label)) },
            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
        )
        OutlinedTextField(
            value = state.startDate,
            onValueChange = viewModel::onStartDateChange,
            label = { Text(stringResource(R.string.tournament_start_date_label)) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
        )
        OutlinedTextField(
            value = state.endDate,
            onValueChange = viewModel::onEndDateChange,
            label = { Text(stringResource(R.string.tournament_end_date_label)) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
        )

        state.errorMessage?.let { Text(it, color = MaterialTheme.colorScheme.error) }

        Button(
            onClick = viewModel::submit,
            enabled = !state.isLoading && state.credits != 0,
            modifier = Modifier.fillMaxWidth(),
        ) {
            if (state.isLoading) {
                CircularProgressIndicator(modifier = Modifier.padding(2.dp), strokeWidth = 2.dp)
            } else {
                Text(stringResource(R.string.tournament_create_button))
            }
        }
    }
}
