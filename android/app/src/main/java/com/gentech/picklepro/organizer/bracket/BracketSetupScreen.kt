package com.gentech.picklepro.organizer.bracket

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.gentech.picklepro.R
import com.gentech.picklepro.core.designsystem.PickleProTopBar

@Composable
fun BracketSetupScreen(
    viewModel: BracketSetupViewModel,
    onGenerated: () -> Unit,
    onViewExistingBracket: () -> Unit,
    onBack: () -> Unit,
) {
    val state by viewModel.uiState.collectAsState()

    LaunchedEffect(state.generated) {
        if (state.generated) onGenerated()
    }

    Scaffold(
        topBar = { PickleProTopBar(stringResource(R.string.bracket_setup_title), onBack) },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (state.division?.locked == true) {
                item {
                    OutlinedButton(onClick = onViewExistingBracket, modifier = Modifier.fillMaxWidth()) {
                        Text(stringResource(R.string.bracket_view_button))
                    }
                }
            }

            if (!state.canRegenerate) {
                item {
                    Text(
                        stringResource(R.string.bracket_regenerate_blocked_note),
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            } else if (state.division?.locked == true) {
                item {
                    Text(stringResource(R.string.bracket_locked_note), color = MaterialTheme.colorScheme.error)
                }
            }

            if (state.unpairedCount > 0) {
                item {
                    Text(stringResource(R.string.bracket_unpaired_warning), color = MaterialTheme.colorScheme.error)
                }
            }

            item { Text(stringResource(R.string.bracket_seed_reorder_hint), style = MaterialTheme.typography.bodyMedium) }

            if (state.isLoading) {
                item { CircularProgressIndicator() }
            } else {
                itemsIndexed(state.seeds, key = { _, e -> e.ref }) { index, entrant ->
                    SeedRow(
                        seedNumber = index + 1,
                        name = entrant.name,
                        rating = entrant.seedRating,
                        canMoveUp = index > 0 && state.canRegenerate,
                        canMoveDown = index < state.seeds.size - 1 && state.canRegenerate,
                        onMoveUp = { viewModel.moveUp(index) },
                        onMoveDown = { viewModel.moveDown(index) },
                    )
                }
            }

            when (state.message) {
                "not_enough" -> item { Text(stringResource(R.string.bracket_not_enough_entrants), color = MaterialTheme.colorScheme.error) }
                "has_results" -> item { Text(stringResource(R.string.bracket_regenerate_blocked_note), color = MaterialTheme.colorScheme.error) }
            }

            if (state.canRegenerate) {
                item {
                    Button(
                        onClick = viewModel::generate,
                        enabled = !state.isGenerating && state.seeds.size >= 2,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        if (state.isGenerating) {
                            CircularProgressIndicator(modifier = Modifier.padding(2.dp), strokeWidth = 2.dp)
                        } else {
                            Text(
                                stringResource(
                                    if (state.division?.locked == true) {
                                        R.string.bracket_regenerate_button
                                    } else {
                                        R.string.bracket_generate_button
                                    },
                                ),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SeedRow(
    seedNumber: Int,
    name: String,
    rating: Double,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(text = "#$seedNumber  $name  (${"%.1f".format(rating)})")
            Row {
                IconButton(onClick = onMoveUp, enabled = canMoveUp) {
                    Icon(Icons.Filled.KeyboardArrowUp, contentDescription = null)
                }
                IconButton(onClick = onMoveDown, enabled = canMoveDown) {
                    Icon(Icons.Filled.KeyboardArrowDown, contentDescription = null)
                }
            }
        }
    }
}
