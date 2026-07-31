package com.gentech.picklepro.organizer.activation

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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import com.gentech.picklepro.R
import com.gentech.picklepro.core.designsystem.PickleProTopBar
import kotlinx.coroutines.delay

@Composable
fun BecomeOrganizerScreen(
    viewModel: BecomeOrganizerViewModel,
    onRedeemed: () -> Unit,
    onBack: () -> Unit,
) {
    val state by viewModel.uiState.collectAsState()

    LaunchedEffect(state.success) {
        if (state.success) {
            // Brief pause so the success message is actually visible before navigating away.
            delay(900)
            onRedeemed()
        }
    }

    Scaffold(
        topBar = { PickleProTopBar(stringResource(R.string.organizer_become_title), onBack) },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                stringResource(R.string.organizer_become_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            OutlinedTextField(
                value = state.code,
                onValueChange = viewModel::onCodeChange,
                label = { Text(stringResource(R.string.organizer_code_label)) },
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                    capitalization = KeyboardCapitalization.Characters,
                ),
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            state.errorMessage?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            if (state.success) {
                Text(stringResource(R.string.organizer_redeem_success), color = MaterialTheme.colorScheme.primary)
            }

            Button(
                onClick = viewModel::submit,
                enabled = !state.isLoading && !state.success,
                modifier = Modifier.fillMaxWidth(),
            ) {
                if (state.isLoading) {
                    CircularProgressIndicator(modifier = Modifier.padding(2.dp), strokeWidth = 2.dp)
                } else {
                    Text(stringResource(R.string.organizer_redeem_button))
                }
            }
        }
    }
}
