package by.mazokaleh.skarnik.feature.search.ui

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import by.mazokaleh.skarnik.core.common.suspendRunCatching
import by.mazokaleh.skarnik.feature.search.domain.SearchWord
import by.mazokaleh.skarnik.feature.search.domain.SearchWordsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn

sealed interface SearchUiState {
    /** Empty query: explains the automatic letter substitution. */
    data object Hint : SearchUiState

    data class Results(val query: String, val words: List<SearchWord>) : SearchUiState

    data class NothingFound(val query: String) : SearchUiState

    data object Error : SearchUiState
}

@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
@HiltViewModel
class SearchViewModel @Inject constructor(private val searchWords: SearchWordsUseCase) : ViewModel() {
    private val query = MutableStateFlow("")

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

    private suspend fun search(query: String): SearchUiState {
        if (query.isEmpty()) return SearchUiState.Hint
        return suspendRunCatching { searchWords(query) }.fold(
            onSuccess = { result ->
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

    private companion object {
        const val TAG = "SearchViewModel"
        val DEBOUNCE = 50.milliseconds
        const val STOP_TIMEOUT_MS = 5_000L
    }
}
