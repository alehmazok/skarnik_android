package by.mazokaleh.skarnik.feature.settings.ui

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import by.mazokaleh.skarnik.core.analytics.AnalyticsConsent
import by.mazokaleh.skarnik.core.common.suspendRunCatching
import by.mazokaleh.skarnik.core.settings.Appearance
import by.mazokaleh.skarnik.core.settings.AppearanceSettings
import by.mazokaleh.skarnik.core.settings.ThemeMode
import by.mazokaleh.skarnik.feature.history.domain.HistoryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * [hasHistory] enables «Ачысціць гісторыю пошуку»; it follows the history store. [analyticsEnabled] is the
 * consent answer (off until the user allows it).
 */
data class GeneralSettingsState(
    val appearance: Appearance = Appearance(),
    val hasHistory: Boolean = false,
    val analyticsEnabled: Boolean = false
)

/** The Settings rows besides offline dictionaries: appearance, analytics and search history. */
@HiltViewModel
class GeneralSettingsViewModel @Inject constructor(
    private val appearanceSettings: AppearanceSettings,
    private val history: HistoryRepository,
    private val analyticsConsent: AnalyticsConsent
) : ViewModel() {
    private val _events = Channel<SettingsEvent>(Channel.BUFFERED)
    val events: Flow<SettingsEvent> = _events.receiveAsFlow()

    val state: StateFlow<GeneralSettingsState> = combine(
        appearanceSettings.appearance.catch {
            Log.e(TAG, "Cannot read the appearance settings", it)
            emit(Appearance())
        },
        history.observe(limit = 1).map { it.words.isNotEmpty() || it.hasMore }.catch {
            Log.e(TAG, "Cannot read history", it)
            emit(false)
        },
        analyticsConsent.state.map { it.granted }.catch {
            Log.e(TAG, "Cannot read the analytics consent", it)
            emit(false)
        }
    ) { appearance, hasHistory, analyticsEnabled -> GeneralSettingsState(appearance, hasHistory, analyticsEnabled) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), GeneralSettingsState())

    fun setThemeMode(themeMode: ThemeMode) {
        viewModelScope.launch {
            suspendRunCatching { appearanceSettings.setThemeMode(themeMode) }
                .onFailure { Log.e(TAG, "Cannot save the theme", it) }
        }
    }

    fun setDynamicColor(enabled: Boolean) {
        viewModelScope.launch {
            suspendRunCatching { appearanceSettings.setDynamicColor(enabled) }
                .onFailure { Log.e(TAG, "Cannot save the colour setting", it) }
        }
    }

    /** The Settings switch counts as an answer too: the first-launch dialog is not shown after it. */
    fun setAnalyticsEnabled(enabled: Boolean) {
        viewModelScope.launch {
            suspendRunCatching { analyticsConsent.setGranted(enabled) }
                .onFailure { Log.e(TAG, "Cannot save the analytics consent", it) }
        }
    }

    fun clearHistory() {
        viewModelScope.launch {
            suspendRunCatching { history.clear() }.fold(
                onSuccess = { _events.send(SettingsEvent.HistoryCleared) },
                onFailure = {
                    Log.e(TAG, "Cannot clear history", it)
                    _events.send(SettingsEvent.HistoryClearFailed)
                }
            )
        }
    }

    private companion object {
        const val TAG = "GeneralSettingsViewModel"
        const val STOP_TIMEOUT_MS = 5_000L
    }
}
