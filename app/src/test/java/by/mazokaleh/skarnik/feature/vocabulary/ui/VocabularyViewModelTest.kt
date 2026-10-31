package by.mazokaleh.skarnik.feature.vocabulary.ui

import app.cash.turbine.ReceiveTurbine
import app.cash.turbine.test
import by.mazokaleh.skarnik.core.model.Dictionary
import by.mazokaleh.skarnik.feature.vocabulary.domain.LoadVocabularyUseCase
import by.mazokaleh.skarnik.feature.vocabulary.domain.VocabularyEntry
import by.mazokaleh.skarnik.feature.vocabulary.domain.VocabularyRepository
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class VocabularyViewModelTest {
    private val requested = mutableListOf<Dictionary>()
    private var failing = false
    private val repository = object : VocabularyRepository {
        override suspend fun entries(dictionary: Dictionary): List<VocabularyEntry> {
            requested += dictionary
            check(!failing) { "corrupt store" }
            return listOf(VocabularyEntry(dictionary.langId, "слова ${dictionary.path}", "с", "слова"))
        }
    }
    private val viewModel by lazy { VocabularyViewModel(LoadVocabularyUseCase(repository, Dispatchers.Unconfined)) }

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `starts on the explanatory dictionary`() = runTest {
        viewModel.uiState.test {
            val state = awaitLoaded()
            assertEquals(Dictionary.Tsbm, state.dictionary)
            assertEquals("слова tsbm", state.ready().sections.single().words.single().word)
        }
    }

    @Test
    fun `a dictionary is loaded once and kept while switching tabs`() = runTest {
        viewModel.uiState.test {
            awaitLoaded()
            viewModel.select(Dictionary.RusBel)
            assertEquals(Dictionary.RusBel, awaitLoaded().dictionary)
            viewModel.select(Dictionary.Tsbm)
            // Straight to Ready: no Loading in between.
            val back = awaitItem()
            assertEquals(Dictionary.Tsbm, back.dictionary)
            back.ready()
        }
        assertEquals(listOf(Dictionary.Tsbm, Dictionary.RusBel), requested)
    }

    @Test
    fun `an error can be retried`() = runTest {
        failing = true
        viewModel.uiState.test {
            assertEquals(VocabularyContent.Error, awaitLoaded().content)
            failing = false
            viewModel.retry()
            awaitLoaded().ready()
        }
    }

    private fun VocabularyUiState.ready() = assertIs<VocabularyContent.Ready>(content).vocabulary

    /** Skips Loading, which may or may not be seen depending on when collection starts. */
    private suspend fun ReceiveTurbine<VocabularyUiState>.awaitLoaded(): VocabularyUiState {
        var item = awaitItem()
        while (item.content == VocabularyContent.Loading) item = awaitItem()
        return item
    }
}
