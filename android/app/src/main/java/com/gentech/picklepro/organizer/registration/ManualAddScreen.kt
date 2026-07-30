package com.gentech.picklepro.organizer.registration

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.gentech.picklepro.R
import com.gentech.picklepro.core.designsystem.PickleProTopBar
import com.gentech.picklepro.data.repository.RejectReason

@Composable
fun ManualAddScreen(viewModel: ManualAddViewModel, onBack: () -> Unit) {
    val state by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = { PickleProTopBar(stringResource(R.string.registration_manual_add_title), onBack) },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            OutlinedTextField(
                value = state.name,
                onValueChange = viewModel::onNameChange,
                label = { Text(stringResource(R.string.registration_manual_add_name_label)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )

            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = stringResource(R.string.registration_manual_add_rating_label).format(state.declaredRating),
                    style = MaterialTheme.typography.labelLarge,
                )
                Slider(
                    value = state.declaredRating,
                    onValueChange = viewModel::onDeclaredRatingChange,
                    valueRange = 2.0f..8.0f,
                    steps = 11,
                )
            }

            state.outcome?.let { outcome ->
                Text(manualAddOutcomeMessage(outcome), color = MaterialTheme.colorScheme.primary)
            }

            Button(
                onClick = viewModel::submit,
                enabled = !state.isLoading && state.name.isNotBlank(),
                modifier = Modifier.fillMaxWidth(),
            ) {
                if (state.isLoading) {
                    CircularProgressIndicator(modifier = Modifier.padding(2.dp), strokeWidth = 2.dp)
                } else {
                    Text(stringResource(R.string.registration_manual_add_button))
                }
            }
        }
    }
}

@Composable
private fun manualAddOutcomeMessage(outcome: ScanOutcomeUi): String = when (outcome) {
    is ScanOutcomeUi.Registered -> stringResource(R.string.registration_scan_registered, outcome.playerName)
    is ScanOutcomeUi.CheckedIn -> stringResource(R.string.registration_scan_checked_in, outcome.playerName)
    is ScanOutcomeUi.Rejected -> when (outcome.reason) {
        RejectReason.LEVEL_GATE -> stringResource(R.string.registration_scan_rejected_gate, outcome.playerName)
        RejectReason.SLOTS_FULL -> stringResource(R.string.registration_scan_rejected_slots)
    }
    is ScanOutcomeUi.Error -> outcome.message
}
