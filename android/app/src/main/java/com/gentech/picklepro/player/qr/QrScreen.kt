package com.gentech.picklepro.player.qr

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.view.WindowManager
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.gentech.picklepro.R
import com.gentech.picklepro.core.qr.PlayerQrPayload
import com.gentech.picklepro.core.qr.QrCodeGenerator

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

@Composable
fun QrScreen(viewModel: QrViewModel) {
    val profile by viewModel.profile.collectAsState()
    var brightnessMaxed by rememberSaveable { mutableStateOf(false) }
    val activity = remember(LocalContext.current) { LocalContext.current.findActivity() }

    ApplyMaxBrightness(activity = activity, enabled = brightnessMaxed)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center,
    ) {
        val shortCode = profile?.shortCode
        val playerId = profile?.id
        if (shortCode == null || playerId == null) {
            CircularProgressIndicator()
            return@Box
        }

        val payload = remember(playerId, shortCode) { PlayerQrPayload(playerId, shortCode).encode() }
        val bitmap = remember(payload) { QrCodeGenerator.generate(payload) }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(text = stringResource(R.string.qr_title), style = MaterialTheme.typography.headlineMedium)
            Text(
                text = stringResource(R.string.qr_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = stringResource(R.string.qr_title),
                modifier = Modifier.size(280.dp),
            )

            Text(text = "${stringResource(R.string.qr_short_code_label)} $shortCode")

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = stringResource(
                        if (brightnessMaxed) R.string.qr_brightness_toggle_off else R.string.qr_brightness_toggle_on,
                    ),
                )
                Switch(checked = brightnessMaxed, onCheckedChange = { brightnessMaxed = it })
            }

            Text(
                text = stringResource(R.string.qr_offline_note),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/**
 * Boosts this window's brightness to max while [enabled], scoped to this
 * screen only — no WRITE_SETTINGS permission needed, and it reverts
 * automatically on toggle-off or when leaving the screen.
 */
@Composable
private fun ApplyMaxBrightness(activity: Activity?, enabled: Boolean) {
    DisposableEffect(activity, enabled) {
        val window = activity?.window
        if (window != null) {
            val params = window.attributes
            params.screenBrightness = if (enabled) 1f else WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE
            window.attributes = params
        }
        onDispose {
            val w = activity?.window ?: return@onDispose
            val params = w.attributes
            params.screenBrightness = WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE
            w.attributes = params
        }
    }
}

