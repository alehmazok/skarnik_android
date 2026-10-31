package by.mazokaleh.skarnik.feature.translation.ui

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import by.mazokaleh.skarnik.core.common.suspendRunCatching
import by.mazokaleh.skarnik.core.model.Dictionary
import by.mazokaleh.skarnik.core.model.WordLink
import by.mazokaleh.skarnik.feature.favorites.domain.BookmarksRepository
import by.mazokaleh.skarnik.feature.history.domain.HistoryRepository
import by.mazokaleh.skarnik.feature.stress.domain.stressCandidates
import by.mazokaleh.skarnik.feature.translation.domain.GetTranslationUseCase
import by.mazokaleh.skarnik.feature.translation.domain.Translation
import by.mazokaleh.skarnik.feature.translation.domain.TranslationResult
import by.mazokaleh.skarnik.feature.translation.domain.WordRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

sealed interface TranslationUiState {
    /** [word] is known when the screen was opened from a list; links and redirects open by id only. */
    data class Loading(val word: String?) : TranslationUiState

    /** [stressCandidates]: the words the «На́ціск» button offers; empty hides it. */
    data class Loaded(
        val translation: Translation,
        val bookmarked: Boolean,
        val stressCandidates: List<String> = emptyList()
    ) : TranslationUiState

    data class Error(val word: String?) : TranslationUiState
}

sealed interface TranslationEvent {
    /** The entry is an alias of [target]: open it in place of this screen. */
    data class Redirect(val target: WordLink, val fromWord: String) : TranslationEvent

    data class BookmarkChanged(val bookmarked: Boolean) : TranslationEvent
}

/**
 * Flutter's `TranslationCubit`: find the word in the index, get its translation through the source chain,
 * save it to history, check the bookmark.
 */
@HiltViewModel(assistedFactory = TranslationViewModel.Factory::class)
class TranslationViewModel @AssistedInject constructor(
    @Assisted private val route: TranslationRoute,
    private val words: WordRepository,
    private val getTranslation: GetTranslationUseCase,
    private val history: HistoryRepository,
    private val bookmarks: BookmarksRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow<TranslationUiState>(TranslationUiState.Loading(route.word))
    val uiState: StateFlow<TranslationUiState> = _uiState.asStateFlow()

    private val _events = Channel<TranslationEvent>(Channel.BUFFERED)
    val events: Flow<TranslationEvent> = _events.receiveAsFlow()

    private var loading: Job? = null

    init {
        load()
    }

    fun retry() {
        if (loading?.isActive != true) load()
    }

    fun toggleBookmark() {
        val loaded = _uiState.value as? TranslationUiState.Loaded ?: return
        val word = loaded.translation.word
        val bookmark = !loaded.bookmarked
        viewModelScope.launch {
            suspendRunCatching { if (bookmark) bookmarks.add(word) else bookmarks.remove(word) }
                .onSuccess {
                    _uiState.update { state ->
                        if (state is TranslationUiState.Loaded) state.copy(bookmarked = bookmark) else state
                    }
                    _events.send(TranslationEvent.BookmarkChanged(bookmark))
                }
                .onFailure { Log.e(TAG, "Cannot change the bookmark", it) }
        }
    }

    private fun load() {
        _uiState.value = TranslationUiState.Loading(route.word)
        loading = viewModelScope.launch {
            _uiState.value = suspendRunCatching { loadTranslation() }.getOrElse {
                Log.e(TAG, "Cannot translate ${route.langId}/${route.wordId}", it)
                TranslationUiState.Error(route.word)
            }
        }
    }

    private suspend fun loadTranslation(): TranslationUiState {
        val dictionary = checkNotNull(Dictionary.byLangId(route.langId)) { "Unknown langId ${route.langId}" }
        val word = checkNotNull(words.find(dictionary, route.wordId)) { "No word ${route.wordId} in the index" }
        return when (val result = getTranslation(word)) {
            is TranslationResult.Redirect -> {
                check(result.target != WordLink(dictionary, word.wordId)) { "Word redirects to itself" }
                _events.send(TranslationEvent.Redirect(result.target, word.word))
                TranslationUiState.Loading(word.word)
            }
            is TranslationResult.Found -> {
                if (route.saveToHistory) {
                    suspendRunCatching { history.save(word) }.onFailure { Log.e(TAG, "Cannot save to history", it) }
                }
                val bookmarked = suspendRunCatching { bookmarks.isBookmarked(word) }.getOrDefault(false)
                val candidates = stressCandidates(word, result.translation.html)
                TranslationUiState.Loaded(result.translation, bookmarked, candidates)
            }
        }
    }

    @AssistedFactory
    interface Factory {
        fun create(route: TranslationRoute): TranslationViewModel
    }

    private companion object {
        const val TAG = "TranslationViewModel"
    }
}
