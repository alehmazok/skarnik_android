package by.mazokaleh.skarnik.feature.vocabulary.ui

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import by.mazokaleh.skarnik.core.common.suspendRunCatching
import by.mazokaleh.skarnik.core.model.Dictionary
import by.mazokaleh.skarnik.feature.vocabulary.domain.LoadVocabularyUseCase
import by.mazokaleh.skarnik.feature.vocabulary.domain.Vocabulary
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.transformLatest

/** The dictionaries in tab order, as in Flutter: the explanatory one first. */
val VOCABULARY_DICTIONARIES = listOf(Dictionary.Tsbm, Dictionary.BelRus, Dictionary.RusBel)

data class VocabularyUiState(val dictionary: Dictionary, val content: VocabularyContent)

sealed interface VocabularyContent {
    data object Loading : VocabularyContent

    data class Ready(val vocabulary: Vocabulary) : VocabularyContent

    /** The store could not be read. */
    data object Error : VocabularyContent
}

/**
 * The Dictionary tab. A dictionary is loaded when its tab is first selected and kept for the life of the home
 * screen, as Flutter's cubit did, so switching tabs back is instant.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class VocabularyViewModel @Inject constructor(private val loadVocabulary: LoadVocabularyUseCase) : ViewModel() {
    private val request = MutableStateFlow(Request(VOCABULARY_DICTIONARIES.first(), attempt = 0))
    private val loaded = mutableMapOf<Dictionary, Vocabulary>()

    val uiState: StateFlow<VocabularyUiState> = request
        .transformLatest { (dictionary) ->
            val cached = loaded[dictionary]
            if (cached != null) {
                emit(VocabularyUiState(dictionary, VocabularyContent.Ready(cached)))
            } else {
                emit(VocabularyUiState(dictionary, VocabularyContent.Loading))
                val content = suspendRunCatching { loadVocabulary(dictionary) }.fold(
                    onSuccess = { vocabulary ->
                        loaded[dictionary] = vocabulary
                        VocabularyContent.Ready(vocabulary)
                    },
                    onFailure = {
                        Log.e(TAG, "Cannot load ${dictionary.path}", it)
                        VocabularyContent.Error
                    }
                )
                emit(VocabularyUiState(dictionary, content))
            }
        }
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
            VocabularyUiState(request.value.dictionary, VocabularyContent.Loading)
        )

    fun select(dictionary: Dictionary) {
        request.value = Request(dictionary, attempt = 0)
    }

    fun retry() {
        request.value = request.value.let { it.copy(attempt = it.attempt + 1) }
    }

    /** [attempt] makes a retry of the same dictionary a new value for the state flow. */
    private data class Request(val dictionary: Dictionary, val attempt: Int)

    private companion object {
        const val TAG = "VocabularyViewModel"
        const val STOP_TIMEOUT_MS = 5_000L
    }
}
