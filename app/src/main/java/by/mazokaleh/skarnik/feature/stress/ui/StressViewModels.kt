package by.mazokaleh.skarnik.feature.stress.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import by.mazokaleh.skarnik.core.analytics.Analytics
import by.mazokaleh.skarnik.core.analytics.AnalyticsEvent
import by.mazokaleh.skarnik.feature.stress.domain.StressResolver
import by.mazokaleh.skarnik.feature.stress.domain.StressResult
import by.mazokaleh.skarnik.feature.stress.domain.StressRow
import by.mazokaleh.skarnik.feature.stress.domain.StressSource
import by.mazokaleh.skarnik.feature.stress.domain.StressWordEntry
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
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable

/** Looks up the stress tables of [word] (a candidate from a translation). */
@Serializable
data class StressRoute(val word: String)

/** One stress table. [word] (stressed) and [tableName] head the screen while the rows load. */
@Serializable
data class StressTableRoute(val id: Long, val source: StressSource, val word: String, val tableName: String?) {
    constructor(entry: StressWordEntry) : this(entry.id, entry.source, entry.word, entry.tableName)
}

sealed interface StressUiState {
    data object Loading : StressUiState

    /** Several forms match (homographs such as «па́ра» / «пара́»): the user picks one. */
    data class Choose(val words: List<StressWordEntry>) : StressUiState

    data object NotFound : StressUiState

    data object Error : StressUiState
}

/**
 * Flutter's `StressCubit`: resolve the word; one match opens its table in place of this screen
 * ([openTable], as Flutter's `context.replace`), several are listed.
 */
@HiltViewModel(assistedFactory = StressViewModel.Factory::class)
class StressViewModel @AssistedInject constructor(
    @Assisted private val route: StressRoute,
    private val resolver: StressResolver,
    analytics: Analytics
) : ViewModel() {
    private val _uiState = MutableStateFlow<StressUiState>(StressUiState.Loading)
    val uiState: StateFlow<StressUiState> = _uiState.asStateFlow()

    private val _openTable = Channel<StressWordEntry>(Channel.BUFFERED)

    /** The single match, to open in place of this screen. */
    val openTable: Flow<StressWordEntry> = _openTable.receiveAsFlow()

    private var loading: Job? = null

    init {
        analytics.log(AnalyticsEvent.StressClicked(route.word))
        load()
    }

    fun retry() {
        if (loading?.isActive != true) load()
    }

    private fun load() {
        _uiState.value = StressUiState.Loading
        loading = viewModelScope.launch {
            _uiState.value = when (val result = resolver.resolveWords(route.word)) {
                is StressResult.Found -> if (result.value.size == 1) {
                    _openTable.send(result.value.single())
                    StressUiState.Loading
                } else {
                    StressUiState.Choose(result.value)
                }
                StressResult.NotFound -> StressUiState.NotFound
                is StressResult.Failed -> StressUiState.Error
            }
        }
    }

    @AssistedFactory
    interface Factory {
        fun create(route: StressRoute): StressViewModel
    }
}

sealed interface StressTableUiState {
    data object Loading : StressTableUiState

    data class Loaded(val rows: List<StressRow>) : StressTableUiState

    data object NotFound : StressTableUiState

    data object Error : StressTableUiState
}

/** Flutter's `StressTableCubit`: the table of one entry, from the source that produced it. */
@HiltViewModel(assistedFactory = StressTableViewModel.Factory::class)
class StressTableViewModel @AssistedInject constructor(
    @Assisted private val route: StressTableRoute,
    private val resolver: StressResolver
) : ViewModel() {
    private val _uiState = MutableStateFlow<StressTableUiState>(StressTableUiState.Loading)
    val uiState: StateFlow<StressTableUiState> = _uiState.asStateFlow()

    private var loading: Job? = null

    init {
        load()
    }

    fun retry() {
        if (loading?.isActive != true) load()
    }

    private fun load() {
        _uiState.value = StressTableUiState.Loading
        loading = viewModelScope.launch {
            _uiState.value = when (val result = resolver.table(route.id, route.source)) {
                is StressResult.Found -> StressTableUiState.Loaded(result.value)
                StressResult.NotFound -> StressTableUiState.NotFound
                is StressResult.Failed -> StressTableUiState.Error
            }
        }
    }

    @AssistedFactory
    interface Factory {
        fun create(route: StressTableRoute): StressTableViewModel
    }
}
