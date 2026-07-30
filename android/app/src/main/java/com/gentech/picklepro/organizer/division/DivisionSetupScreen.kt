package com.gentech.picklepro.organizer.division

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.gentech.picklepro.R
import com.gentech.picklepro.core.designsystem.PickleProTopBar
import com.gentech.picklepro.data.remote.dto.DivisionDto

private val AGE_BRACKET_OPTIONS = listOf("19+", "35+", "50+", "60+")

@Composable
fun DivisionSetupScreen(
    viewModel: DivisionSetupViewModel,
    onOpenRegistrations: (divisionId: String) -> Unit,
    onOpenBracket: (divisionId: String, alreadyGenerated: Boolean) -> Unit,
    onOpenResults: (divisionId: String) -> Unit,
    onBack: () -> Unit,
) {
    val state by viewModel.uiState.collectAsState()
    val form = state.form

    if (form != null) {
        DivisionFormView(
            form = form,
            isSaving = state.isSaving,
            errorMessage = state.errorMessage,
            onUpdate = viewModel::updateForm,
            onSave = viewModel::saveForm,
            onCancel = viewModel::cancelForm,
        )
        return
    }

    Scaffold(
        topBar = { PickleProTopBar(stringResource(R.string.division_setup_title), onBack) },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Button(onClick = viewModel::startNewDivision, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.division_add_button))
                }
            }
            state.errorMessage?.let { message ->
                item { Text(message, color = MaterialTheme.colorScheme.error) }
            }
            if (state.isLoading) {
                item { CircularProgressIndicator() }
            } else if (state.divisions.isEmpty()) {
                item { Text(stringResource(R.string.division_empty), style = MaterialTheme.typography.bodyMedium) }
            } else {
                items(state.divisions, key = { it.id }) { division ->
                    DivisionRow(
                        division = division,
                        onEdit = { viewModel.startEditDivision(division) },
                        onDelete = { viewModel.deleteDivision(division.id) },
                        onManageRegistrations = { onOpenRegistrations(division.id) },
                        onManageBracket = { onOpenBracket(division.id, division.locked) },
                        onManageResults = { onOpenResults(division.id) },
                    )
                }
            }
        }
    }
}

@Composable
private fun DivisionRow(
    division: DivisionDto,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onManageRegistrations: () -> Unit,
    onManageBracket: () -> Unit,
    onManageResults: () -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(text = division.name, style = MaterialTheme.typography.titleLarge)
            val gate = division.maxRating?.let { "≤ $it" } ?: stringResource(R.string.division_skill_gate_open)
            val age = division.ageBracket ?: stringResource(R.string.division_age_bracket_open)
            Text(
                text = "${eventTypeLabel(division.eventType)} • $gate • $age",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = "${formatLabel(division.format)} • Game to ${division.gameTo} • ${scoringModeLabel(division.scoringMode)}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = onManageRegistrations) { Text(stringResource(R.string.division_manage_registrations)) }
                TextButton(onClick = onManageBracket) { Text(stringResource(R.string.division_manage_bracket)) }
                TextButton(onClick = onManageResults) { Text(stringResource(R.string.division_manage_results)) }
            }
            if (division.locked) {
                Text(
                    text = stringResource(R.string.division_locked_note),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                )
            } else {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(onClick = onEdit) { Text(stringResource(R.string.common_edit)) }
                    TextButton(onClick = onDelete) { Text(stringResource(R.string.division_delete_button)) }
                }
            }
        }
    }
}

@Composable
private fun DivisionFormView(
    form: DivisionFormState,
    isSaving: Boolean,
    errorMessage: String?,
    onUpdate: ((DivisionFormState) -> DivisionFormState) -> Unit,
    onSave: () -> Unit,
    onCancel: () -> Unit,
) {
    Scaffold(
        topBar = {
            PickleProTopBar(
                title = if (form.id == null) stringResource(R.string.division_add_button) else form.name,
                onBack = onCancel,
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            OutlinedTextField(
                value = form.name,
                onValueChange = { v -> onUpdate { it.copy(name = v) } },
                label = { Text(stringResource(R.string.division_name_label)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )

            LabeledChipRow(stringResource(R.string.division_event_type_label)) {
                ChoiceChip("singles", stringResource(R.string.event_type_singles), form.eventType) { v -> onUpdate { it.copy(eventType = v) } }
                ChoiceChip("doubles", stringResource(R.string.event_type_doubles), form.eventType) { v -> onUpdate { it.copy(eventType = v) } }
                ChoiceChip("mixed", stringResource(R.string.event_type_mixed), form.eventType) { v -> onUpdate { it.copy(eventType = v) } }
            }

            SwitchRow(
                label = stringResource(R.string.division_skill_gate_label),
                checked = form.skillGateEnabled,
                onCheckedChange = { v -> onUpdate { it.copy(skillGateEnabled = v) } },
            )
            if (form.skillGateEnabled) {
                OutlinedTextField(
                    value = form.maxRating,
                    onValueChange = { v -> onUpdate { it.copy(maxRating = v) } },
                    label = { Text(stringResource(R.string.division_max_rating_label)) },
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                )
            }

            SwitchRow(
                label = stringResource(R.string.division_age_bracket_label),
                checked = form.ageBracketEnabled,
                onCheckedChange = { v -> onUpdate { it.copy(ageBracketEnabled = v) } },
            )
            if (form.ageBracketEnabled) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    AGE_BRACKET_OPTIONS.forEach { bracket ->
                        ChoiceChip(bracket, bracket, form.ageBracket) { v -> onUpdate { it.copy(ageBracket = v) } }
                    }
                }
            }

            OutlinedTextField(
                value = form.maxSlots,
                onValueChange = { v -> onUpdate { it.copy(maxSlots = v.filter(Char::isDigit)) } },
                label = { Text(stringResource(R.string.division_max_slots_label)) },
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )

            LabeledChipRow(stringResource(R.string.division_format_label)) {
                ChoiceChip("single_elim", stringResource(R.string.division_format_single_elim), form.format) { v -> onUpdate { it.copy(format = v) } }
                ChoiceChip("round_robin", stringResource(R.string.division_format_round_robin), form.format) { v -> onUpdate { it.copy(format = v) } }
            }

            LabeledChipRow(stringResource(R.string.division_game_to_label)) {
                ChoiceChip("11", "11", form.gameTo.toString()) { v -> onUpdate { it.copy(gameTo = v.toInt()) } }
                ChoiceChip("15", "15", form.gameTo.toString()) { v -> onUpdate { it.copy(gameTo = v.toInt()) } }
                ChoiceChip("21", "21", form.gameTo.toString()) { v -> onUpdate { it.copy(gameTo = v.toInt()) } }
            }

            SwitchRow(
                label = stringResource(R.string.division_win_by_2_label),
                checked = form.winBy2,
                onCheckedChange = { v -> onUpdate { it.copy(winBy2 = v) } },
            )

            LabeledChipRow(stringResource(R.string.division_best_of_label)) {
                ChoiceChip("1", "1", form.bestOf.toString()) { v -> onUpdate { it.copy(bestOf = v.toInt()) } }
                ChoiceChip("3", "3", form.bestOf.toString()) { v -> onUpdate { it.copy(bestOf = v.toInt()) } }
            }

            LabeledChipRow(stringResource(R.string.division_scoring_mode_label)) {
                ChoiceChip("sideout", stringResource(R.string.division_scoring_mode_sideout), form.scoringMode) { v -> onUpdate { it.copy(scoringMode = v) } }
                ChoiceChip("rally", stringResource(R.string.division_scoring_mode_rally), form.scoringMode) { v -> onUpdate { it.copy(scoringMode = v) } }
            }

            if (form.format == "single_elim") {
                SwitchRow(
                    label = stringResource(R.string.division_bronze_match_label),
                    checked = form.bronzeMatch,
                    onCheckedChange = { v -> onUpdate { it.copy(bronzeMatch = v) } },
                )
            }

            errorMessage?.let { Text(it, color = MaterialTheme.colorScheme.error) }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = onSave, enabled = !isSaving, modifier = Modifier.weight(1f)) {
                    Text(stringResource(R.string.division_save_button))
                }
                TextButton(onClick = onCancel, modifier = Modifier.weight(1f)) {
                    Text(stringResource(R.string.common_cancel))
                }
            }
        }
    }
}

@Composable
private fun LabeledChipRow(label: String, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(label, style = MaterialTheme.typography.labelLarge)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { content() }
    }
}

@Composable
private fun ChoiceChip(value: String, label: String, selected: String, onSelect: (String) -> Unit) {
    FilterChip(selected = value == selected, onClick = { onSelect(value) }, label = { Text(label) })
}

@Composable
private fun SwitchRow(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, style = MaterialTheme.typography.labelLarge)
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun eventTypeLabel(eventType: String): String = when (eventType) {
    "doubles" -> stringResource(R.string.event_type_doubles)
    "mixed" -> stringResource(R.string.event_type_mixed)
    else -> stringResource(R.string.event_type_singles)
}

@Composable
private fun formatLabel(format: String): String = when (format) {
    "round_robin" -> stringResource(R.string.division_format_round_robin)
    else -> stringResource(R.string.division_format_single_elim)
}

@Composable
private fun scoringModeLabel(mode: String): String = when (mode) {
    "rally" -> stringResource(R.string.division_scoring_mode_rally)
    else -> stringResource(R.string.division_scoring_mode_sideout)
}
