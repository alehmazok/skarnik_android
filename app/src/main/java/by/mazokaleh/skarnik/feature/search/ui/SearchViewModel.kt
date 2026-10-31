package by.mazokaleh.skarnik.feature.search.ui

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import by.mazokaleh.skarnik.core.analytics.Analytics
import by.mazokaleh.skarnik.core.analytics.AnalyticsEvent
import by.mazokaleh.skarnik.core.common.suspendRunCatching
import by.mazokaleh.skarnik.feature.search.domain.SearchWord
import by.mazokaleh.skarnik.feature.search.domain.SearchWordsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface SearchUiState {
    /** Empty query: explains the automatic letter substitution. */
    data object Hint : SearchUiState

    data class Results(val query: String, val words: List<SearchWord>) : SearchUiState

    data class NothingFound(val query: String) : SearchUiState

    data object Error : SearchUiState
}

@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
@HiltViewModel
class SearchViewModel @Inject constructor(
    private val searchWords: SearchWordsUseCase,
    private val analytics: Analytics
) : ViewModel() {
    private val query = MutableStateFlow("")
    private var searchAnalytics: Job? = null

    val uiState: StateFlow<SearchUiState> = query
        .map { it.trim() }
        .distinctUntilChanged()
        // Typing fast should not queue a search per keystroke (Flutter: 50 ms).
        .debounce(DEBOUNCE)
        .mapLatest(::search)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), SearchUiState.Hint)

    fun onQueryChange(text: String) {
        query.value = text
    }

    /** A tapped result row; [position] is its index from 0. */
    fun onResultClick(word: SearchWord, position: Int) {
        val shown = uiState.value as? SearchUiState.Results ?: return
        analytics.log(AnalyticsEvent.SearchResultTapped(word, position, shown.query))
    }

    private suspend fun search(query: String): SearchUiState {
        searchAnalytics?.cancel()
        if (query.isEmpty()) return SearchUiState.Hint
        return suspendRunCatching { searchWords(query) }.fold(
            onSuccess = { result ->
                logSearchLater(query, result.words.size, result.usedPrepositionFallback)
                if (result.words.isEmpty()) {
                    SearchUiState.NothingFound(query)
                } else {
                    SearchUiState.Results(query, result.words)
                }
            },
            onFailure = {
                Log.e(TAG, "Search failed for '$query'", it)
                SearchUiState.Error
            }
        )
    }

    /**
     * Flutter logs a search only once its results stayed for [ANALYTICS_DELAY], so typing a word logs the word,
     * not every prefix. The next keystroke cancels the pending event.
     */
    private fun logSearchLater(query: String, resultCount: Int, usedPrepositionFallback: Boolean) {
        searchAnalytics = viewModelScope.launch {
            delay(ANALYTICS_DELAY)
            analytics.log(
                if (resultCount > 0) {
                    AnalyticsEvent.SearchPerformed(query, resultCount, usedPrepositionFallback)
                } else {
                    AnalyticsEvent.SearchNoResults(query, usedPrepositionFallback)
                }
            )
        }
    }

    private companion object {
        const val TAG = "SearchViewModel"
        val DEBOUNCE = 50.milliseconds
        val ANALYTICS_DELAY = 500.milliseconds
        const val STOP_TIMEOUT_MS = 5_000L
    }
}
