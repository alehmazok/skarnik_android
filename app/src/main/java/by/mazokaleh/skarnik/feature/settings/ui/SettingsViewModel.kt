package by.mazokaleh.skarnik.feature.settings.ui

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import by.mazokaleh.skarnik.core.common.suspendRunCatching
import by.mazokaleh.skarnik.core.model.Dictionary
import by.mazokaleh.skarnik.feature.offline.domain.CheckDownloadRateLimitUseCase
import by.mazokaleh.skarnik.feature.offline.domain.DeleteOfflineDictionaryUseCase
import by.mazokaleh.skarnik.feature.offline.domain.OfflineDictionarySettings
import by.mazokaleh.skarnik.feature.offline.domain.OfflineDictionaryStatus
import by.mazokaleh.skarnik.feature.offline.domain.OfflineDictionaryStore
import by.mazokaleh.skarnik.feature.offline.domain.OfflineDownloads
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

data class OfflineDictionaryItem(val dictionary: Dictionary, val status: OfflineDictionaryStatus)

sealed interface OfflineSectionState {
    data object Loading : OfflineSectionState

    data class Ready(val items: List<OfflineDictionaryItem>) : OfflineSectionState

    data object Error : OfflineSectionState
}

sealed interface SettingsEvent {
    data object RateLimited : SettingsEvent

    data object Deleted : SettingsEvent

    data object DeleteFailed : SettingsEvent

    data object HistoryCleared : SettingsEvent

    data object HistoryClearFailed : SettingsEvent
}

/**
 * The «Афлайн доступ» section of Settings. Downloads run in WorkManager, so the rows follow the job, the
 * store and the resume cursor rather than anything this ViewModel holds: leaving the screen never stops a
 * download (Flutter kept an app-wide cubit for that). Opening the screen clears the promo badge.
 */
@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val downloads: OfflineDownloads,
    private val settings: OfflineDictionarySettings,
    private val checkRateLimit: CheckDownloadRateLimitUseCase,
    private val deleteDictionary: DeleteOfflineDictionaryUseCase,
    store: OfflineDictionaryStore
) : ViewModel() {
    private val _events = Channel<SettingsEvent>(Channel.BUFFERED)
    val events: Flow<SettingsEvent> = _events.receiveAsFlow()

    val offline: StateFlow<OfflineSectionState> = combine(Dictionary.entries.map { item(it, store) }) { it.toList() }
        .map<_, OfflineSectionState> { OfflineSectionState.Ready(it) }
        .catch {
            Log.e(TAG, "Cannot read offline dictionaries", it)
            emit(OfflineSectionState.Error)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), OfflineSectionState.Loading)

    init {
        viewModelScope.launch {
            suspendRunCatching { settings.markPromoSeen() }.onFailure { Log.e(TAG, "Cannot save the promo flag", it) }
        }
    }

    private fun item(dictionary: Dictionary, store: OfflineDictionaryStore): Flow<OfflineDictionaryItem> = combine(
        downloads.observe(dictionary),
        store.observeCount(dictionary),
        settings.cursor(dictionary)
    ) { job, count, cursor -> OfflineDictionaryItem(dictionary, OfflineDictionaryStatus.of(job, count, cursor)) }

    fun download(dictionary: Dictionary) {
        val status = (offline.value as? OfflineSectionState.Ready)?.items?.find { it.dictionary == dictionary }?.status
        if (status is OfflineDictionaryStatus.Downloading || status is OfflineDictionaryStatus.Downloaded) return
        viewModelScope.launch {
            // TODO(parity 5.6): log `offline_dictionary_download_click` with the analytics/consent PR.
            suspendRunCatching { checkRateLimit() }.fold(
                onSuccess = { allowed ->
                    if (allowed) downloads.start(dictionary) else _events.send(SettingsEvent.RateLimited)
                },
                onFailure = { Log.e(TAG, "Cannot check the download rate limit", it) }
            )
        }
    }

    fun delete(dictionary: Dictionary) {
        viewModelScope.launch {
            suspendRunCatching { deleteDictionary(dictionary) }.fold(
                onSuccess = { _events.send(SettingsEvent.Deleted) },
                onFailure = {
                    Log.e(TAG, "Cannot delete ${dictionary.path}", it)
                    _events.send(SettingsEvent.DeleteFailed)
                }
            )
        }
    }

    private companion object {
        const val TAG = "SettingsViewModel"
        const val STOP_TIMEOUT_MS = 5_000L
    }
}

/** Whether the settings button shows the offline dictionaries badge: until Settings were opened once. */
@HiltViewModel
class SettingsBadgeViewModel @Inject constructor(settings: OfflineDictionarySettings) : ViewModel() {
    val showBadge: StateFlow<Boolean> = settings.promoSeen
        .map { !it }
        .catch {
            Log.e(TAG, "Cannot read the promo flag", it)
            emit(false)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), false)

    private companion object {
        const val TAG = "SettingsBadgeViewModel"
        const val STOP_TIMEOUT_MS = 5_000L
    }
}
