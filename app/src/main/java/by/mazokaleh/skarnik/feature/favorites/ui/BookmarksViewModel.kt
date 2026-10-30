package by.mazokaleh.skarnik.feature.favorites.ui

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import by.mazokaleh.skarnik.core.common.suspendRunCatching
import by.mazokaleh.skarnik.core.model.SAVED_WORDS_PAGE_SIZE
import by.mazokaleh.skarnik.core.model.SavedWord
import by.mazokaleh.skarnik.feature.favorites.domain.BookmarksRepository
import by.mazokaleh.skarnik.feature.favorites.domain.BookmarksSortOrder
import by.mazokaleh.skarnik.feature.favorites.domain.BookmarksSortRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface BookmarksUiState {
    data object Loading : BookmarksUiState

    data class Ready(val words: List<SavedWord>, val hasMore: Boolean, val sortOrder: BookmarksSortOrder) :
        BookmarksUiState

    data object Error : BookmarksUiState
}

sealed interface BookmarksEvent {
    /** Shows «Выдалена з закладак.» with «Вярнуць». */
    data class Removed(val bookmark: SavedWord) : BookmarksEvent
}

/**
 * Bookmarks in the remembered sort order. The list observes the store, so bookmarks added or removed on the
 * translation screen show at once (Flutter's kept-alive tab never reloaded). A removal is written at once and
 * undone by putting the row back with its old id.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class BookmarksViewModel @Inject constructor(
    private val bookmarks: BookmarksRepository,
    private val sortRepository: BookmarksSortRepository
) : ViewModel() {
    private val limit = MutableStateFlow(SAVED_WORDS_PAGE_SIZE)

    private val _events = Channel<BookmarksEvent>(Channel.BUFFERED)
    val events: Flow<BookmarksEvent> = _events.receiveAsFlow()

    val uiState: StateFlow<BookmarksUiState> = sortRepository.sortOrder
        .flatMapLatest { sortOrder ->
            // A new order starts from the first page again. The previous order's flow is already cancelled here.
            limit.value = SAVED_WORDS_PAGE_SIZE
            limit.flatMapLatest { bookmarks.observe(sortOrder, it) }
                .map<_, BookmarksUiState> { BookmarksUiState.Ready(it.words, it.hasMore, sortOrder) }
        }
        .catch {
            Log.e(TAG, "Cannot read bookmarks", it)
            emit(BookmarksUiState.Error)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), BookmarksUiState.Loading)

    fun loadMore() {
        val state = uiState.value
        if (state is BookmarksUiState.Ready && state.hasMore && state.words.size >= limit.value) {
            limit.value += SAVED_WORDS_PAGE_SIZE
        }
    }

    fun setSortOrder(sortOrder: BookmarksSortOrder) {
        viewModelScope.launch {
            suspendRunCatching { sortRepository.setSortOrder(sortOrder) }
                .onFailure { Log.e(TAG, "Cannot save the sort order", it) }
        }
    }

    fun remove(bookmark: SavedWord) {
        viewModelScope.launch {
            suspendRunCatching { bookmarks.remove(bookmark) }.fold(
                onSuccess = { _events.send(BookmarksEvent.Removed(bookmark)) },
                onFailure = { Log.e(TAG, "Cannot remove a bookmark", it) }
            )
        }
    }

    fun undoRemove(bookmark: SavedWord) {
        viewModelScope.launch {
            suspendRunCatching { bookmarks.restore(bookmark) }
                .onFailure { Log.e(TAG, "Cannot restore a bookmark", it) }
        }
    }

    private companion object {
        const val TAG = "BookmarksViewModel"
        const val STOP_TIMEOUT_MS = 5_000L
    }
}
