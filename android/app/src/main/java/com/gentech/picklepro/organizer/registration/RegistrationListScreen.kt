package com.gentech.picklepro.organizer.registration

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
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
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
fun RegistrationListScreen(
    viewModel: RegistrationListViewModel,
    onScanQr: () -> Unit,
    onManualAdd: () -> Unit,
    onPairing: () -> Unit,
) {
    val state by viewModel.uiState.collectAsState()
    val isSingles = state.division?.eventType == "singles"

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Text(stringResource(R.string.registration_list_title), style = MaterialTheme.typography.headlineMedium)
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                OutlinedButton(onClick = onScanQr, modifier = Modifier.weight(1f)) {
                    Text(stringResource(R.string.registration_scan_qr_button))
                }
                OutlinedButton(onClick = onManualAdd, modifier = Modifier.weight(1f)) {
                    Text(stringResource(R.string.registration_manual_add_nav_button))
                }
            }
        }
        if (!isSingles) {
            item {
                OutlinedButton(onClick = onPairing, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.registration_pairing_button))
                }
            }
        }
        item {
            OutlinedTextField(
                value = state.searchQuery,
                onValueChange = viewModel::onSearchChange,
                label = { Text(stringResource(R.string.registration_search_hint)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )
        }
        if (state.isLoading) {
            item { CircularProgressIndicator() }
        } else if (state.filtered.isEmpty()) {
            item { Text(stringResource(R.string.registration_empty), style = MaterialTheme.typography.bodyMedium) }
        } else {
            items(state.filtered, key = { it.id }) { registration ->
                RegistrationRow(
                    registration = registration,
                    isDoublesDivision = !isSingles,
                    onToggleCheckIn = { checked -> viewModel.toggleCheckIn(registration.id, checked) },
                    onUnregister = { viewModel.unregister(registration.id) },
                )
            }
        }
    }
}

@Composable
private fun RegistrationRow(
    registration: RegistrationWithProfileDto,
    isDoublesDivision: Boolean,
    onToggleCheckIn: (Boolean) -> Unit,
    onUnregister: () -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(text = registration.profiles.name, style = MaterialTheme.typography.titleLarge)
                    if (registration.profiles.isShell) {
                        AssistChip(onClick = {}, label = { Text(stringResource(R.string.registration_shell_badge)) })
                    }
                }
                if (isDoublesDivision && registration.teamId == null) {
                    Text(
                        text = stringResource(R.string.registration_unpaired_label),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
                TextButton(onClick = onUnregister) {
                    Text(stringResource(R.string.registration_unregister_button))
                }
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(stringResource(R.string.registration_check_in_label), style = MaterialTheme.typography.bodyMedium)
                Switch(checked = registration.checkedIn, onCheckedChange = onToggleCheckIn)
            }
        }
    }
}
