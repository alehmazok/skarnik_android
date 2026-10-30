package by.mazokaleh.skarnik.feature.history.ui

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import by.mazokaleh.skarnik.core.model.SAVED_WORDS_PAGE_SIZE
import by.mazokaleh.skarnik.core.model.SavedWord
import by.mazokaleh.skarnik.feature.history.domain.HistoryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

sealed interface HistoryUiState {
    data object Loading : HistoryUiState

    data class Ready(val words: List<SavedWord>, val hasMore: Boolean) : HistoryUiState

    /** The stores could not be opened. */
    data object Error : HistoryUiState
}

/**
 * The words opened, newest first. The list observes the store, so a word opened anywhere shows at once
 * (Flutter reloaded on an app-wide event instead). Pages grow by [SAVED_WORDS_PAGE_SIZE] as the user scrolls.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class HistoryViewModel @Inject constructor(private val history: HistoryRepository) : ViewModel() {
    private val limit = MutableStateFlow(SAVED_WORDS_PAGE_SIZE)

    val uiState: StateFlow<HistoryUiState> = limit
        .flatMapLatest { history.observe(it) }
        .map<_, HistoryUiState> { HistoryUiState.Ready(it.words, it.hasMore) }
        .catch {
            Log.e(TAG, "Cannot read history", it)
            emit(HistoryUiState.Error)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), HistoryUiState.Loading)

    /** Called when the end of the list shows; does nothing until the current page has loaded. */
    fun loadMore() {
        val state = uiState.value
        if (state is HistoryUiState.Ready && state.hasMore && state.words.size >= limit.value) {
            limit.value += SAVED_WORDS_PAGE_SIZE
        }
    }

    private companion object {
        const val TAG = "HistoryViewModel"
        const val STOP_TIMEOUT_MS = 5_000L
    }
}
