package by.mazokaleh.skarnik.feature.whatsnew.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import by.mazokaleh.skarnik.core.analytics.AnalyticsConsent
import by.mazokaleh.skarnik.core.common.suspendRunCatching
import by.mazokaleh.skarnik.core.crash.logError
import by.mazokaleh.skarnik.feature.whatsnew.domain.GetWhatsNewUseCase
import by.mazokaleh.skarnik.feature.whatsnew.domain.WhatsNewRelease
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * The release to show in the sheet after an update, or null. It waits for the analytics consent answer, so the
 * sheet never covers the consent dialog. The view model outlives rotation, so the sheet stays open across it.
 */
@HiltViewModel
class WhatsNewViewModel @Inject constructor(getWhatsNew: GetWhatsNewUseCase, consent: AnalyticsConsent) : ViewModel() {
    private val _release = MutableStateFlow<WhatsNewRelease?>(null)
    val release: StateFlow<WhatsNewRelease?> = _release.asStateFlow()

    init {
        viewModelScope.launch {
            val toShow = getWhatsNew() ?: return@launch
            suspendRunCatching { consent.state.first { it.answered } }
                .onSuccess { _release.value = toShow }
                .onFailure { logError(TAG, "Cannot read the analytics consent", it) }
        }
    }

    fun dismiss() {
        _release.value = null
    }

    private companion object {
        const val TAG = "WhatsNew"
    }
}
