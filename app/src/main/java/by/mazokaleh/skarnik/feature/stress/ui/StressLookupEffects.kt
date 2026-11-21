package by.mazokaleh.skarnik.feature.stress.ui

import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.LocalResources
import by.mazokaleh.skarnik.R
import by.mazokaleh.skarnik.feature.stress.domain.StressWordEntry
import kotlinx.coroutines.launch

/**
 * «На́ціск» results that leave the sheet: the table to open, or a snackbar when nothing is found or the lookup
 * failed (with «Паўтарыць»). skarnik_ios shows alerts; M3 uses a snackbar for a short message with one action.
 */
@Composable
fun StressLookupEffects(
    stressLookup: StressLookupViewModel,
    snackbarHostState: SnackbarHostState,
    onOpenStressTable: (StressWordEntry) -> Unit
) {
    val resources = LocalResources.current
    val currentOnOpenStressTable by rememberUpdatedState(onOpenStressTable)
    LaunchedEffect(stressLookup) {
        stressLookup.events.collect { event ->
            when (event) {
                is StressLookupEvent.OpenTable -> currentOnOpenStressTable(event.entry)
                StressLookupEvent.NotFound -> {
                    snackbarHostState.currentSnackbarData?.dismiss()
                    launch { snackbarHostState.showSnackbar(resources.getString(R.string.stress_not_found)) }
                }
                is StressLookupEvent.Failed -> {
                    snackbarHostState.currentSnackbarData?.dismiss()
                    launch {
                        val result = snackbarHostState.showSnackbar(
                            message = resources.getString(R.string.stress_error),
                            actionLabel = resources.getString(R.string.action_retry),
                            duration = SnackbarDuration.Long
                        )
                        if (result == SnackbarResult.ActionPerformed) stressLookup.retry(event.word)
                    }
                }
            }
        }
    }
}
