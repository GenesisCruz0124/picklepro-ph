package com.gentech.picklepro.player.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.gentech.picklepro.R
import com.gentech.picklepro.core.rating.tierLabelRes
import com.gentech.picklepro.data.local.entity.RatingEntity
import com.gentech.picklepro.data.repository.MatchHistoryItem
import com.gentech.picklepro.data.repository.MatchOutcome

@Composable
fun ProfileScreen(viewModel: ProfileViewModel) {
    val state by viewModel.uiState.collectAsState()
    val profile = state.profile

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                if (profile?.photoUrl != null) {
                    AsyncImage(
                        model = profile.photoUrl,
                        contentDescription = null,
                        modifier = Modifier
                            .size(88.dp)
                            .clip(CircleShape),
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .size(88.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Person,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(48.dp),
                        )
                    }
                }
                Text(
                    text = profile?.name?.ifBlank { "…" } ?: "…",
                    style = MaterialTheme.typography.headlineMedium,
                )
                profile?.location?.takeIf { it.isNotBlank() }?.let {
                    Text(text = it, style = MaterialTheme.typography.bodyMedium)
                }
                if (profile?.duprVerified == true) {
                    AssistChip(
                        onClick = {},
                        label = { Text(stringResource(R.string.profile_dupr_verified_badge)) },
                        leadingIcon = {
                            Icon(Icons.Filled.Verified, contentDescription = null, modifier = Modifier.size(16.dp))
                        },
                    )
                }
            }
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                state.ratings.forEach { rating -> TierBadge(rating) }
            }
        }

        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = stringResource(R.string.profile_rating_chart_title),
                    style = MaterialTheme.typography.titleLarge,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    EventTypeTab("singles", R.string.event_type_singles, state.selectedEventType, viewModel::selectEventType)
                    EventTypeTab("doubles", R.string.event_type_doubles, state.selectedEventType, viewModel::selectEventType)
                    EventTypeTab("mixed", R.string.event_type_mixed, state.selectedEventType, viewModel::selectEventType)
                }
                RatingHistoryChart(points = state.ratingHistory.map { it.eloAfter.let { elo -> 2.0 + (elo - 800) / 400.0 } })
            }
        }

        item {
            Text(
                text = stringResource(R.string.profile_match_history_title),
                style = MaterialTheme.typography.titleLarge,
            )
        }

        if (state.isLoadingMatchHistory) {
            item {
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
        } else if (state.matchHistory.isEmpty()) {
            item {
                Text(
                    text = stringResource(R.string.profile_match_history_empty),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        } else {
            items(state.matchHistory, key = { it.matchId }) { match ->
                MatchHistoryRow(match)
            }
        }
    }
}

@Composable
private fun TierBadge(rating: RatingEntity) {
    val display = rating.override ?: rating.display
    AssistChip(
        onClick = {},
        label = {
            Text(
                stringResource(tierLabelRes(display)) +
                    if (rating.provisional) " " + stringResource(R.string.tier_provisional_suffix) else "",
            )
        },
    )
}

@Composable
private fun EventTypeTab(
    value: String,
    @androidx.annotation.StringRes labelRes: Int,
    selected: String,
    onSelect: (String) -> Unit,
) {
    FilterChip(
        selected = value == selected,
        onClick = { onSelect(value) },
        label = { Text(stringResource(labelRes)) },
    )
}

@Composable
private fun MatchHistoryRow(match: MatchHistoryItem) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column {
                Text(text = "vs ${match.opponentName}", style = MaterialTheme.typography.titleLarge)
                Text(
                    text = "${match.tournamentName} • ${match.divisionName}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = match.games.joinToString(", ") { (mine, theirs) -> "$mine-$theirs" },
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            Text(
                text = stringResource(
                    if (match.outcome == MatchOutcome.WIN) R.string.profile_win else R.string.profile_loss,
                ),
                color = if (match.outcome == MatchOutcome.WIN) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.error
                },
                style = MaterialTheme.typography.titleLarge,
            )
        }
    }
}
