package com.gentech.picklepro.organizer.wallet

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.gentech.picklepro.R

private const val MESSENGER_USERNAME = "genesiscruz0124"
private const val BUY_MORE_MESSAGE =
    "Hi Gen! Gusto ko pong bumili ng tournament activation code para sa PicklePro PH."

@Composable
fun WalletScreen(
    viewModel: WalletViewModel,
    onRedeemAnother: () -> Unit,
) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(stringResource(R.string.organizer_wallet_title), style = MaterialTheme.typography.headlineMedium)

        if (state.isLoading) {
            CircularProgressIndicator()
        } else {
            Text(text = "${state.credits ?: 0}", style = MaterialTheme.typography.displayLarge)
            Text(
                text = stringResource(R.string.organizer_wallet_credits_label),
                style = MaterialTheme.typography.titleLarge,
            )
        }

        Text(
            text = stringResource(R.string.organizer_wallet_credits_hint),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Button(onClick = onRedeemAnother, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.organizer_wallet_redeem_another))
        }

        OutlinedButton(
            onClick = {
                val uri = Uri.parse("https://m.me/$MESSENGER_USERNAME?text=${Uri.encode(BUY_MORE_MESSAGE)}")
                context.startActivity(Intent(Intent.ACTION_VIEW, uri))
            },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(stringResource(R.string.organizer_wallet_buy_more))
        }
    }
}
