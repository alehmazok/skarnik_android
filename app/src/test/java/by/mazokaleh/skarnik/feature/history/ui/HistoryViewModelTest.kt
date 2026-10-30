package by.mazokaleh.skarnik.feature.history.ui

import app.cash.turbine.ReceiveTurbine
import app.cash.turbine.test
import by.mazokaleh.skarnik.core.model.Dictionary
import by.mazokaleh.skarnik.core.model.SAVED_WORDS_PAGE_SIZE
import by.mazokaleh.skarnik.core.model.SavedWord
import by.mazokaleh.skarnik.core.model.SavedWordsPage
import by.mazokaleh.skarnik.core.model.Word
import by.mazokaleh.skarnik.feature.history.domain.HistoryRepository
import kotlin.test.assertEquals
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class HistoryViewModelTest {
    private val stored = MutableStateFlow(words(3))
    private val requestedLimits = mutableListOf<Int>()
    private val repository = object : HistoryRepository {
        override suspend fun save(word: Word) = Unit

        override fun observe(limit: Int): Flow<SavedWordsPage> {
            requestedLimits += limit
            return stored.map { SavedWordsPage(it.take(limit), hasMore = it.size > limit) }
        }
    }

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `shows history and follows changes in the store`() = runTest {
        val viewModel = HistoryViewModel(repository)

        viewModel.uiState.test {
            assertEquals(HistoryUiState.Ready(words(3), hasMore = false), awaitLoaded())
            stored.value = words(4)
            assertEquals(HistoryUiState.Ready(words(4), hasMore = false), awaitItem())
        }
    }

    @Test
    fun `loads the next page only once the current one is shown`() = runTest {
        stored.value = words(SAVED_WORDS_PAGE_SIZE * 2 + 1)
        val viewModel = HistoryViewModel(repository)

        viewModel.uiState.test {
            assertEquals(SAVED_WORDS_PAGE_SIZE, (awaitLoaded() as HistoryUiState.Ready).words.size)
            viewModel.loadMore()
            val second = awaitItem() as HistoryUiState.Ready
            assertEquals(SAVED_WORDS_PAGE_SIZE * 2, second.words.size)
            assertEquals(true, second.hasMore)
            viewModel.loadMore()
            val last = awaitItem() as HistoryUiState.Ready
            assertEquals(SAVED_WORDS_PAGE_SIZE * 2 + 1, last.words.size)
            assertEquals(false, last.hasMore)
            viewModel.loadMore()
            expectNoEvents()
        }
        assertEquals(listOf(25, 50, 75), requestedLimits)
    }

    @Test
    fun `shows an error when the stores cannot be opened`() = runTest {
        val failing = object : HistoryRepository {
            override suspend fun save(word: Word) = Unit

            override fun observe(limit: Int): Flow<SavedWordsPage> = flow { error("corrupt store") }
        }

        HistoryViewModel(failing).uiState.test {
            assertEquals(HistoryUiState.Error, awaitLoaded())
        }
    }

    /** Skips the initial Loading, which may or may not be seen depending on when collection starts. */
    private suspend fun ReceiveTurbine<HistoryUiState>.awaitLoaded(): HistoryUiState {
        var item = awaitItem()
        while (item == HistoryUiState.Loading) item = awaitItem()
        return item
    }

    private fun words(count: Int) = (count downTo 1).map {
        SavedWord(id = it.toLong(), dictionary = Dictionary.BelRus, wordId = it * 10L, word = "слова $it", letter = "с")
    }
}
