package by.mazokaleh.skarnik.feature.update.ui

import android.app.Activity
import android.util.Log
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import by.mazokaleh.skarnik.core.analytics.Analytics
import by.mazokaleh.skarnik.core.analytics.AnalyticsConsent
import by.mazokaleh.skarnik.core.analytics.AnalyticsEvent
import by.mazokaleh.skarnik.core.common.suspendRunCatching
import by.mazokaleh.skarnik.core.crash.logError
import by.mazokaleh.skarnik.feature.update.domain.AppUpdateAction
import by.mazokaleh.skarnik.feature.update.domain.AppUpdateType
import by.mazokaleh.skarnik.feature.update.domain.AppUpdates
import by.mazokaleh.skarnik.feature.update.domain.ChooseAppUpdateUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Checks Play for an update each time the app comes to the front, once the analytics consent dialog has been
 * answered (so Play's prompt never covers it), and asks the screen to start the flow [ChooseAppUpdateUseCase] picks.
 * Logs `app_update_prompt_shown` / `accepted` / `dismissed` / `restart_clicked`.
 */
@HiltViewModel
class AppUpdateViewModel @Inject constructor(
    private val updates: AppUpdates,
    private val chooseUpdate: ChooseAppUpdateUseCase,
    private val consent: AnalyticsConsent,
    private val analytics: Analytics
) : ViewModel() {
    private val _request = MutableStateFlow<AppUpdateType?>(null)

    /** A flow the screen should start with its activity result launcher ([launch]), or null. */
    val request: StateFlow<AppUpdateType?> = _request.asStateFlow()

    private val _restartReady = MutableStateFlow(false)

    /** A flexible update is downloaded: the screen offers a restart until the user answers. */
    val restartReady: StateFlow<Boolean> = _restartReady.asStateFlow()

    private var checking: Job? = null
    private var requestedVersionCode = 0L

    /** The prompt Play is showing, until its result arrives. */
    private var shown: AppUpdateAnalytics? = null

    /** The immediate flow is not restarted in a session where the user closed it; it is at the next start. */
    private var immediateDeclined = false

    init {
        viewModelScope.launch {
            suspendRunCatching { updates.downloaded.collect { _restartReady.value = true } }
                .onFailure { Log.w(TAG, "Cannot follow the update download", it) }
        }
    }

    fun onResume() {
        if (checking?.isActive == true || _request.value != null || shown != null) return
        checking = viewModelScope.launch {
            suspendRunCatching {
                consent.state.first { it.answered }
                val status = updates.check() ?: return@launch
                when (val action = chooseUpdate(status, immediateDeclined)) {
                    AppUpdateAction.None -> Unit
                    AppUpdateAction.OfferRestart -> _restartReady.value = true
                    is AppUpdateAction.Start -> {
                        requestedVersionCode = status.availableVersionCode
                        _request.value = action.type
                    }
                }
            }.onFailure { logError(TAG, "Cannot check for an app update", it) }
        }
    }

    fun launch(type: AppUpdateType, launcher: ActivityResultLauncher<IntentSenderRequest>) {
        if (_request.value != type) return
        _request.value = null
        if (updates.start(type, launcher)) {
            val prompt = AppUpdateAnalytics(type, requestedVersionCode)
            shown = prompt
            analytics.log(AnalyticsEvent.AppUpdatePromptShown(prompt.type, prompt.availableVersionCode))
        }
    }

    /**
     * Play's result: OK when the user chose to update (an immediate update usually restarts the app before this
     * arrives), CANCELED when they closed the prompt, anything else when the flow failed.
     */
    fun onFlowResult(resultCode: Int) {
        val prompt = shown ?: return
        shown = null
        when (resultCode) {
            Activity.RESULT_OK ->
                analytics.log(AnalyticsEvent.AppUpdateAccepted(prompt.type, prompt.availableVersionCode))
            Activity.RESULT_CANCELED ->
                analytics.log(AnalyticsEvent.AppUpdateDismissed(prompt.type, prompt.availableVersionCode))
            else -> Log.w(TAG, "The ${prompt.type.analyticsName} update failed: $resultCode")
        }
        if (prompt.type == AppUpdateType.Immediate && resultCode != Activity.RESULT_OK) immediateDeclined = true
    }

    fun restart() {
        _restartReady.value = false
        analytics.log(AnalyticsEvent.AppUpdateRestartClicked)
        updates.completeUpdate()
    }

    /** Closed without a restart: offered again the next time the app comes to the front. */
    fun restartDismissed() {
        _restartReady.value = false
    }

    private data class AppUpdateAnalytics(val type: AppUpdateType, val availableVersionCode: Long)

    private companion object {
        const val TAG = "AppUpdate"
    }
}
