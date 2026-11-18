package by.mazokaleh.skarnik.feature.update.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import by.mazokaleh.skarnik.R

/**
 * Google Play in-app updates (parity 8.13, docs/adr/0016): starts Play's prompt when there is an update, and once a
 * flexible update is downloaded shows «Абнаўленне гатова» with «Перазапусціць» in [snackbarHostState], the app-wide
 * snackbar the home screen shows above its navigation bar.
 */
@Composable
fun AppUpdateHost(snackbarHostState: SnackbarHostState, viewModel: AppUpdateViewModel = hiltViewModel()) {
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) {
        viewModel.onFlowResult(it.resultCode)
    }
    LifecycleResumeEffect(viewModel) {
        viewModel.onResume()
        onPauseOrDispose {}
    }

    val request by viewModel.request.collectAsStateWithLifecycle()
    LaunchedEffect(request) {
        request?.let { viewModel.launch(it, launcher) }
    }

    val restartReady by viewModel.restartReady.collectAsStateWithLifecycle()
    val message = stringResource(R.string.app_update_downloaded)
    val action = stringResource(R.string.app_update_restart)
    LaunchedEffect(restartReady) {
        if (!restartReady) return@LaunchedEffect
        val result = snackbarHostState.showSnackbar(
            message = message,
            actionLabel = action,
            withDismissAction = true,
            duration = SnackbarDuration.Indefinite
        )
        if (result == SnackbarResult.ActionPerformed) viewModel.restart() else viewModel.restartDismissed()
    }
}
