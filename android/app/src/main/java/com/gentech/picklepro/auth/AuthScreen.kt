package com.gentech.picklepro.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.PasswordVisualTransformation
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.gentech.picklepro.R

@Composable
fun AuthScreen(viewModel: AuthViewModel) {
    val state by viewModel.uiState.collectAsState()
    // Navigating away on successful sign-in is driven by AuthRepository.authState
    // (see PickleProNavHost) so this screen only needs to render its own state.

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            text = stringResource(
                if (state.mode == AuthMode.LOGIN) R.string.auth_login_title else R.string.auth_signup_title,
            ),
            style = MaterialTheme.typography.headlineMedium,
        )

        if (state.mode == AuthMode.SIGNUP) {
            OutlinedTextField(
                value = state.name,
                onValueChange = viewModel::onNameChange,
                label = { Text(stringResource(R.string.auth_full_name)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )
        }

        OutlinedTextField(
            value = state.email,
            onValueChange = viewModel::onEmailChange,
            label = { Text(stringResource(R.string.auth_email)) },
            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Email),
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
        )

        OutlinedTextField(
            value = state.password,
            onValueChange = viewModel::onPasswordChange,
            label = { Text(stringResource(R.string.auth_password)) },
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Password),
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
        )

        if (state.mode == AuthMode.SIGNUP) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = stringResource(R.string.auth_declared_rating_label) +
                        " — %.1f".format(state.declaredRating),
                    style = MaterialTheme.typography.labelLarge,
                )
                Slider(
                    value = state.declaredRating,
                    onValueChange = viewModel::onDeclaredRatingChange,
                    valueRange = 2.0f..8.0f,
                    steps = 11, // 0.5 increments across 2.0..8.0
                )
                Text(
                    text = stringResource(R.string.auth_declared_rating_hint),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = stringResource(R.string.auth_event_preferences_label),
                    style = MaterialTheme.typography.labelLarge,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    EventTypeChip("singles", R.string.event_type_singles, state.selectedEventTypes, viewModel::onToggleEventType)
                    EventTypeChip("doubles", R.string.event_type_doubles, state.selectedEventTypes, viewModel::onToggleEventType)
                    EventTypeChip("mixed", R.string.event_type_mixed, state.selectedEventTypes, viewModel::onToggleEventType)
                }
            }
        }

        state.errorMessage?.let { message ->
            Text(text = message, color = MaterialTheme.colorScheme.error)
        }
        if (state.awaitingEmailConfirmation) {
            Text(text = stringResource(R.string.auth_check_email))
        }

        Button(
            onClick = viewModel::submit,
            enabled = !state.isLoading,
            modifier = Modifier.fillMaxWidth(),
        ) {
            if (state.isLoading) {
                CircularProgressIndicator(modifier = Modifier.padding(2.dp), strokeWidth = 2.dp)
            } else {
                Text(
                    stringResource(
                        if (state.mode == AuthMode.LOGIN) R.string.auth_login_button else R.string.auth_signup_button,
                    ),
                )
            }
        }

        TextButton(
            onClick = viewModel::onToggleMode,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(
                stringResource(
                    if (state.mode == AuthMode.LOGIN) R.string.auth_switch_to_signup else R.string.auth_switch_to_login,
                ),
            )
        }
    }
}

@Composable
private fun EventTypeChip(
    value: String,
    @androidx.annotation.StringRes labelRes: Int,
    selected: Set<String>,
    onToggle: (String) -> Unit,
) {
    FilterChip(
        selected = value in selected,
        onClick = { onToggle(value) },
        label = { Text(stringResource(labelRes)) },
    )
}
