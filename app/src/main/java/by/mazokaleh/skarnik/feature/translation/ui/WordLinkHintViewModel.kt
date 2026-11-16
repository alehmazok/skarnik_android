package by.mazokaleh.skarnik.feature.translation.ui

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import by.mazokaleh.skarnik.core.analytics.Analytics
import by.mazokaleh.skarnik.core.analytics.AnalyticsEvent
import by.mazokaleh.skarnik.core.common.suspendRunCatching
import by.mazokaleh.skarnik.feature.translation.domain.WordLinkHintSettings
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * The one-time hint about tappable words (Aleh, 2026-11-16): the first translation with such words underlines them
 * and shows a tooltip, once per install. It counts as shown as soon as it appears, so leaving the screen does not
 * bring it back; it stays on this screen until it is closed or a word is tapped. Storage errors mean "no hint".
 */
@HiltViewModel
class WordLinkHintViewModel @Inject constructor(
    private val settings: WordLinkHintSettings,
    private val analytics: Analytics
) : ViewModel() {
    private val _visible = MutableStateFlow(false)

    /** True while the hint and the underlines show. */
    val visible: StateFlow<Boolean> = _visible.asStateFlow()

    private var asked = false

    /** The screen has words to tap: show the hint unless it was shown before. */
    fun onWordLinksShown() {
        if (asked) return
        asked = true
        viewModelScope.launch {
            val shown = suspendRunCatching { settings.isHintShown() }.getOrElse {
                Log.w(TAG, "Cannot read the word link hint flag", it)
                true
            }
            if (shown) return@launch
            suspendRunCatching { settings.markHintShown() }.onFailure { Log.w(TAG, "Cannot save the hint flag", it) }
            _visible.value = true
            analytics.log(AnalyticsEvent.WordLinkHintShown)
        }
    }

    /** The hint closed: [byTap] when a word was tapped. */
    fun onHintDone(byTap: Boolean) {
        if (!_visible.value) return
        _visible.value = false
        analytics.log(AnalyticsEvent.WordLinkHintDismissed(byTap))
    }

    private companion object {
        const val TAG = "WordLinkHint"
    }
}
