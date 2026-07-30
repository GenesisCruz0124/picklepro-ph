package com.gentech.picklepro.organizer.tournament

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.gentech.picklepro.R
import com.gentech.picklepro.core.designsystem.PickleProTopBar
import com.gentech.picklepro.organizer.common.nextTournamentStatus
import com.gentech.picklepro.organizer.common.tournamentStatusLabelRes

@Composable
fun TournamentDetailScreen(
    viewModel: TournamentDetailViewModel,
    onOpenDivisions: () -> Unit,
    onBack: () -> Unit,
) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    val logoPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
            ?: return@rememberLauncherForActivityResult
        val mimeType = context.contentResolver.getType(uri)
        val extension = when {
            mimeType?.contains("png") == true -> "png"
            mimeType?.contains("webp") == true -> "webp"
            else -> "jpg"
        }
        viewModel.uploadLogo(bytes, extension)
    }

    val tournament = state.tournament

    Scaffold(
        topBar = { PickleProTopBar(stringResource(R.string.tournament_setup_title_edit), onBack) },
    ) { padding ->
        if (state.isLoading || tournament == null) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                if (tournament.logoUrl != null) {
                    AsyncImage(
                        model = tournament.logoUrl,
                        contentDescription = null,
                        modifier = Modifier.size(96.dp),
                    )
                }
                OutlinedButton(
                    onClick = { logoPicker.launch("image/*") },
                    enabled = !state.isUploadingLogo,
                ) {
                    if (state.isUploadingLogo) {
                        CircularProgressIndicator(modifier = Modifier.padding(2.dp), strokeWidth = 2.dp)
                    } else {
                        Text(stringResource(R.string.tournament_logo_upload))
                    }
                }
            }

            StatusRow(
                status = tournament.status,
                isBusy = state.isSaving,
                onAdvance = viewModel::advanceStatus,
            )

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

            Button(onClick = viewModel::save, enabled = !state.isSaving, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.common_save))
            }

            OutlinedButton(onClick = onOpenDivisions, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.tournament_divisions_button))
            }
        }
    }
}

@Composable
private fun StatusRow(status: String, isBusy: Boolean, onAdvance: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(stringResource(R.string.tournament_status_label), style = MaterialTheme.typography.labelLarge)
        AssistChip(onClick = {}, label = { Text(stringResource(tournamentStatusLabelRes(status))) })
        val next = nextTournamentStatus(status)
        if (next != null) {
            TextButton(onClick = onAdvance, enabled = !isBusy) {
                Text("→ " + stringResource(tournamentStatusLabelRes(next)))
            }
        }
    }
}
