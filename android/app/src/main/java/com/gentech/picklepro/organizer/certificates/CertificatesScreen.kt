package com.gentech.picklepro.organizer.certificates

import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import com.gentech.picklepro.R
import com.gentech.picklepro.core.designsystem.PickleProTopBar

@Composable
fun CertificatesScreen(viewModel: CertificatesViewModel, onBack: () -> Unit) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    val championLabel = stringResource(R.string.certificate_kind_champion)
    val runnerUpLabel = stringResource(R.string.certificate_kind_runner_up)
    val participationLabel = stringResource(R.string.certificate_kind_participation)
    val awardedToLabel = stringResource(R.string.certificate_awarded_to)
    val organizedByLabel = stringResource(R.string.certificate_organized_by)
    val shareTitle = stringResource(R.string.certificates_share_title)

    LaunchedEffect(state.fileToShare) {
        val file = state.fileToShare ?: return@LaunchedEffect
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, shareTitle))
        viewModel.onShared()
    }

    fun kindLabelFor(kind: CertificateKind): String = when (kind) {
        CertificateKind.CHAMPION -> championLabel
        CertificateKind.RUNNER_UP -> runnerUpLabel
        CertificateKind.PARTICIPATION -> participationLabel
    }

    Scaffold(
        topBar = { PickleProTopBar(stringResource(R.string.certificates_title), onBack) },
    ) { padding ->
        if (state.isLoading) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Text(
                    stringResource(R.string.certificates_hint),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            state.errorMessage?.let { message ->
                item { Text(message, color = MaterialTheme.colorScheme.error) }
            }

            items(state.podiumItems, key = { "${it.kind}-${it.recipientRef}" }) { item ->
                CertificateRow(
                    item = item,
                    kindLabel = kindLabelFor(item.kind),
                    isGenerating = state.generatingRef == item.recipientRef,
                    onGenerate = {
                        viewModel.generate(item, kindLabelFor(item.kind), awardedToLabel, organizedByLabel)
                    },
                )
            }

            if (state.participationItems.isNotEmpty()) {
                item {
                    Text(
                        stringResource(R.string.certificates_participation_section),
                        style = MaterialTheme.typography.titleLarge,
                    )
                }
                items(state.participationItems, key = { "${it.kind}-${it.recipientRef}" }) { item ->
                    CertificateRow(
                        item = item,
                        kindLabel = kindLabelFor(item.kind),
                        isGenerating = state.generatingRef == item.recipientRef,
                        onGenerate = {
                            viewModel.generate(item, kindLabelFor(item.kind), awardedToLabel, organizedByLabel)
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun CertificateRow(
    item: CertificateItem,
    kindLabel: String,
    isGenerating: Boolean,
    onGenerate: () -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth(), onClick = onGenerate, enabled = !isGenerating) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column {
                Text(item.recipientName, style = MaterialTheme.typography.titleLarge)
                Text(
                    kindLabel,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            if (isGenerating) {
                CircularProgressIndicator(modifier = Modifier.padding(4.dp), strokeWidth = 2.dp)
            }
        }
    }
}
