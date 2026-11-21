package by.mazokaleh.skarnik.feature.stress.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import by.mazokaleh.skarnik.core.analytics.Analytics
import by.mazokaleh.skarnik.core.analytics.AnalyticsEvent
import by.mazokaleh.skarnik.feature.stress.domain.StressPickerSection
import by.mazokaleh.skarnik.feature.stress.domain.StressResolver
import by.mazokaleh.skarnik.feature.stress.domain.StressResult
import by.mazokaleh.skarnik.feature.stress.domain.StressRow
import by.mazokaleh.skarnik.feature.stress.domain.StressSource
import by.mazokaleh.skarnik.feature.stress.domain.StressWordEntry
import by.mazokaleh.skarnik.feature.stress.domain.stressPickerSections
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable

/**
 * One stress table. [word] (stressed), [tableName] and the full [meaning] (the picker cuts it to two lines) head
 * the screen while the rows load.
 */
@Serializable
data class StressTableRoute(
    val id: Long,
    val source: StressSource,
    val word: String,
    val tableName: String?,
    val meaning: String? = null
) {
    constructor(entry: StressWordEntry) : this(entry.id, entry.source, entry.word, entry.tableName, entry.meaning)
}

/**
 * The homonym picker sheet: several entries match (homonyms such as «па́ра» / «пара́»), and the user picks one by
 * stressed form ([sections]) and meaning.
 */
data class StressPicker(val word: String, val sections: List<StressPickerSection>) {
    /** No entry has a meaning (always so for skarnik_admin): the sheet says so under the list. */
    val noMeanings: Boolean get() = sections.all { section -> section.meaningCount == 0 }
}

/** [loadingWord] is being looked up (the chip shows progress); [picker] is the open homonym sheet, if any. */
data class StressLookupUiState(val loadingWord: String? = null, val picker: StressPicker? = null)

sealed interface StressLookupEvent {
    /** The single match, or the one picked in the sheet. */
    data class OpenTable(val entry: StressWordEntry) : StressLookupEvent

    data object NotFound : StressLookupEvent

    /** Both sources failed; the snackbar offers a retry. */
    data class Failed(val word: String) : StressLookupEvent
}

/**
 * «На́ціск» on a translation (Flutter's `StressCubit`, skarnik_ios `openSpellingWord`): resolves a candidate in
 * place. One match opens its table, several open the picker sheet, nothing or an error shows a snackbar. One
 * lookup at a time; scoped to the translation, so a picker survives rotation.
 */
@HiltViewModel
class StressLookupViewModel @Inject constructor(
    private val resolver: StressResolver,
    private val analytics: Analytics
) : ViewModel() {
    private val _uiState = MutableStateFlow(StressLookupUiState())
    val uiState: StateFlow<StressLookupUiState> = _uiState.asStateFlow()

    private val _events = Channel<StressLookupEvent>(Channel.BUFFERED)
    val events: Flow<StressLookupEvent> = _events.receiveAsFlow()

    private var loading: Job? = null

    /** A tap on the chip: logs `stress_clicked`, unlike [retry]. */
    fun lookup(word: String) {
        if (loading?.isActive == true) return
        analytics.log(AnalyticsEvent.StressClicked(word))
        load(word)
    }

    fun retry(word: String) {
        if (loading?.isActive != true) load(word)
    }

    /** A row of the sheet; only the first tap counts, since the sheet closes with it. */
    fun choose(entry: StressWordEntry) {
        if (_uiState.value.picker == null) return
        _uiState.update { it.copy(picker = null) }
        _events.trySend(StressLookupEvent.OpenTable(entry))
    }

    fun dismissPicker() {
        _uiState.update { it.copy(picker = null) }
    }

    private fun load(word: String) {
        _uiState.update { StressLookupUiState(loadingWord = word) }
        loading = viewModelScope.launch {
            val result = resolver.resolveWords(word)
            _uiState.update { it.copy(loadingWord = null) }
            when (result) {
                is StressResult.Found -> if (result.value.size == 1) {
                    _events.send(StressLookupEvent.OpenTable(result.value.single()))
                } else {
                    _uiState.update { it.copy(picker = StressPicker(word, stressPickerSections(result.value))) }
                }
                StressResult.NotFound -> _events.send(StressLookupEvent.NotFound)
                is StressResult.Failed -> _events.send(StressLookupEvent.Failed(word))
            }
        }
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
