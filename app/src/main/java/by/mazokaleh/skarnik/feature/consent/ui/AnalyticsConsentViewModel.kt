package by.mazokaleh.skarnik.feature.consent.ui

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import by.mazokaleh.skarnik.core.analytics.AnalyticsConsent
import by.mazokaleh.skarnik.core.common.suspendRunCatching
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Flutter's first-launch consent dialog: shown until the user answers. [showDialog] is false while the answer is
 * read, so the dialog never flashes for someone who already answered; an unreadable store asks again.
 */
@HiltViewModel
class AnalyticsConsentViewModel @Inject constructor(private val consent: AnalyticsConsent) : ViewModel() {
    /** Hides the dialog as soon as a button is tapped, before the store confirms the write. */
    private val answeredHere = MutableStateFlow(false)

    val showDialog: StateFlow<Boolean> = combine(
        consent.state.map { !it.answered }.catch {
            Log.e(TAG, "Cannot read the analytics consent", it)
            emit(true)
        },
        answeredHere
    ) { notAnswered, answered -> notAnswered && !answered }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), false)

    fun answer(granted: Boolean) {
        answeredHere.value = true
        viewModelScope.launch {
            suspendRunCatching { consent.setGranted(granted) }
                .onFailure { Log.e(TAG, "Cannot save the analytics consent", it) }
        }
    }

    private companion object {
        const val TAG = "AnalyticsConsent"
        const val STOP_TIMEOUT_MS = 5_000L
    }
}
