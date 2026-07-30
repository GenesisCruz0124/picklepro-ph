package com.gentech.picklepro.organizer.registration

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.gentech.picklepro.R
import com.gentech.picklepro.data.repository.RejectReason

@Composable
fun QrScanScreen(viewModel: QrScanViewModel) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    var hasPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED,
        )
    }
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        hasPermission = granted
    }

    Box(modifier = Modifier.fillMaxSize()) {
        if (!hasPermission) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(stringResource(R.string.registration_camera_permission_needed))
                Button(onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) }) {
                    Text(stringResource(R.string.registration_camera_permission_grant))
                }
            }
        } else if (state.isScanning) {
            CameraPreview(onBarcodeDetected = viewModel::onBarcodeDetected)
        }

        if (state.isProcessing) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }

        state.outcome?.let { outcome ->
            Card(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(16.dp),
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(outcomeMessage(outcome), style = MaterialTheme.typography.titleLarge)
                    Button(onClick = viewModel::scanAnother, modifier = Modifier.fillMaxWidth()) {
                        Text(stringResource(R.string.registration_scan_another))
                    }
                }
            }
        }
    }
}

@Composable
private fun outcomeMessage(outcome: ScanOutcomeUi): String = when (outcome) {
    is ScanOutcomeUi.Registered -> stringResource(R.string.registration_scan_registered, outcome.playerName)
    is ScanOutcomeUi.CheckedIn -> stringResource(R.string.registration_scan_checked_in, outcome.playerName)
    is ScanOutcomeUi.Rejected -> when (outcome.reason) {
        RejectReason.LEVEL_GATE -> stringResource(R.string.registration_scan_rejected_gate, outcome.playerName)
        RejectReason.SLOTS_FULL -> stringResource(R.string.registration_scan_rejected_slots)
    }
    is ScanOutcomeUi.Error -> outcome.message
}
