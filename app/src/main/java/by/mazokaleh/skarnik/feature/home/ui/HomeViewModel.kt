package by.mazokaleh.skarnik.feature.home.ui

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import by.mazokaleh.skarnik.core.common.suspendRunCatching
import by.mazokaleh.skarnik.feature.home.data.DictionaryStatsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface HomeUiState {
    data object Loading : HomeUiState

    data class Ready(val wordCount: Long) : HomeUiState

    data object Error : HomeUiState
}

/** Placeholder home: proves the stores open. Replaced by the history tab in the search/history PRs. */
@HiltViewModel
class HomeViewModel @Inject constructor(private val statsRepository: DictionaryStatsRepository) : ViewModel() {
    private val _uiState = MutableStateFlow<HomeUiState>(HomeUiState.Loading)
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            _uiState.value = suspendRunCatching { statsRepository.searchWordCount() }.fold(
                onSuccess = { HomeUiState.Ready(it) },
                onFailure = {
                    Log.e(TAG, "Cannot open the database", it)
                    HomeUiState.Error
                }
            )
        }
    }

    private companion object {
        const val TAG = "HomeViewModel"
    }
}
