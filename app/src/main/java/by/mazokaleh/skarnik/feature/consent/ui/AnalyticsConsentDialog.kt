package by.mazokaleh.skarnik.feature.consent.ui

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import by.mazokaleh.skarnik.R

/** Asks for analytics consent over whatever screen is open, until the user answers (parity 8.6). */
@Composable
fun AnalyticsConsentHost(viewModel: AnalyticsConsentViewModel = hiltViewModel()) {
    val showDialog by viewModel.showDialog.collectAsStateWithLifecycle()
    if (showDialog) AnalyticsConsentDialog(onAnswer = viewModel::answer)
}

/**
 * Not dismissible by back or a tap outside: the user makes an explicit choice (as Flutter). Both answers are
 * equal text buttons, so declining is as easy as allowing.
 */
@Composable
fun AnalyticsConsentDialog(onAnswer: (granted: Boolean) -> Unit) {
    AlertDialog(
        onDismissRequest = {},
        properties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false),
        title = { Text(stringResource(R.string.analytics_consent_title)) },
        text = { Text(stringResource(R.string.analytics_consent_message)) },
        confirmButton = {
            TextButton(onClick = { onAnswer(true) }) { Text(stringResource(R.string.analytics_consent_accept)) }
        },
        dismissButton = {
            TextButton(onClick = { onAnswer(false) }) { Text(stringResource(R.string.analytics_consent_decline)) }
        }
    )
}
