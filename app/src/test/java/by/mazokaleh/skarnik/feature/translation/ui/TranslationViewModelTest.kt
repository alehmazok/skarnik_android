package by.mazokaleh.skarnik.feature.translation.ui

import app.cash.turbine.test
import by.mazokaleh.skarnik.core.model.Dictionary
import by.mazokaleh.skarnik.core.model.SavedWord
import by.mazokaleh.skarnik.core.model.SavedWordsPage
import by.mazokaleh.skarnik.core.model.Word
import by.mazokaleh.skarnik.core.model.WordLink
import by.mazokaleh.skarnik.feature.favorites.domain.BookmarksRepository
import by.mazokaleh.skarnik.feature.favorites.domain.BookmarksSortOrder
import by.mazokaleh.skarnik.feature.history.domain.HistoryRepository
import by.mazokaleh.skarnik.feature.translation.domain.GetTranslationUseCase
import by.mazokaleh.skarnik.feature.translation.domain.RemoteTranslationSource
import by.mazokaleh.skarnik.feature.translation.domain.Translation
import by.mazokaleh.skarnik.feature.translation.domain.TranslationEntry
import java.io.IOException
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class TranslationViewModelTest {
    private val word = Word(Dictionary.BelRus, 27890, "бурштын", "б", "бурштын", null)
    private val route = TranslationRoute(langId = 1, wordId = 27890, word = "бурштын")
    private val history = mutableListOf<Word>()
    private val bookmarks = FakeBookmarks()
    private var api: RemoteTranslationSource = RemoteTranslationSource { _, _ ->
        TranslationEntry(stress = null, html = "янтарь", redirectTo = null)
    }

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun viewModel(route: TranslationRoute = this.route) = TranslationViewModel(
        route = route,
        words = { dictionary, id -> word.takeIf { it.dictionary == dictionary && it.wordId == id } },
        getTranslation = GetTranslationUseCase(
            local = { _, _ -> null },
            api = { dictionary, id -> api.fetch(dictionary, id) },
            cloud = { _, _ -> throw IOException("offline") }
        ),
        history = object : HistoryRepository {
            override suspend fun save(word: Word) {
                history += word
            }

            override suspend fun clear() = Unit

            override fun observe(limit: Int): Flow<SavedWordsPage> = emptyFlow()
        },
        bookmarks = bookmarks
    )

    @Test
    fun `loads the translation and saves the word to history`() = runTest {
        val state = viewModel().uiState.value

        val loaded = assertIs<TranslationUiState.Loaded>(state)
        assertEquals("янтарь", loaded.translation.html)
        assertEquals(false, loaded.bookmarked)
        assertEquals(listOf(word), history)
    }

    @Test
    fun `offers the stress candidates of the translation`() = runTest {
        api = RemoteTranslationSource { _, _ ->
            TranslationEntry(stress = null, html = "<font color=\"#831b03\">янтар, бурштын</font>", redirectTo = null)
        }

        val loaded = assertIs<TranslationUiState.Loaded>(viewModel().uiState.value)

        assertEquals(listOf("бурштын", "янтар"), loaded.stressCandidates)
    }

    @Test
    fun `does not save to history when opened from history`() = runTest {
        viewModel(route.copy(saveToHistory = false))

        assertEquals(emptyList(), history)
    }

    @Test
    fun `shows an error and retries`() = runTest {
        api = RemoteTranslationSource { _, _ -> throw IOException("offline") }
        val viewModel = viewModel()
        assertEquals(TranslationUiState.Error("бурштын"), viewModel.uiState.value)

        api = RemoteTranslationSource { _, _ -> TranslationEntry(null, "янтарь", null) }
        viewModel.retry()

        assertIs<TranslationUiState.Loaded>(viewModel.uiState.value)
    }

    @Test
    fun `a word missing from the index is an error`() = runTest {
        val state = viewModel(TranslationRoute(langId = 1, wordId = 1)).uiState.value

        assertEquals(TranslationUiState.Error(null), state)
        assertEquals(emptyList(), history)
    }

    @Test
    fun `a redirect asks to open the target and is not saved to history`() = runTest {
        api = RemoteTranslationSource { _, _ -> TranslationEntry(null, null, "https://skarnik.by/tsbm/3528") }
        val viewModel = viewModel()

        viewModel.events.test {
            assertEquals(TranslationEvent.Redirect(WordLink(Dictionary.Tsbm, 3528), "бурштын"), awaitItem())
        }
        assertEquals(emptyList(), history)
    }

    @Test
    fun `a word that redirects to itself is an error`() = runTest {
        api = RemoteTranslationSource { _, _ -> TranslationEntry(null, null, "/belrus/27890") }

        assertEquals(TranslationUiState.Error("бурштын"), viewModel().uiState.value)
    }

    @Test
    fun `toggles the bookmark and reports it`() = runTest {
        val viewModel = viewModel()

        viewModel.events.test {
            viewModel.toggleBookmark()
            assertEquals(TranslationEvent.BookmarkChanged(bookmarked = true), awaitItem())
            assertEquals(setOf(word), bookmarks.words)
            assertEquals(true, (viewModel.uiState.value as TranslationUiState.Loaded).bookmarked)

            viewModel.toggleBookmark()
            assertEquals(TranslationEvent.BookmarkChanged(bookmarked = false), awaitItem())
            assertEquals(emptySet(), bookmarks.words)
        }
    }

    @Test
    fun `shows an existing bookmark`() = runTest {
        bookmarks.words += word

        assertEquals(true, (viewModel().uiState.value as TranslationUiState.Loaded).bookmarked)
    }

    @Test
    fun `the title carries the stress mark`() = runTest {
        api = RemoteTranslationSource { _, _ -> TranslationEntry("буршты́н", "янтарь", null) }

        val translation: Translation = (viewModel().uiState.value as TranslationUiState.Loaded).translation
        assertEquals("буршты́н", translation.title)
    }

    private class FakeBookmarks : BookmarksRepository {
        val words = mutableSetOf<Word>()

        override suspend fun isBookmarked(word: Word) = word in words

        override suspend fun add(word: Word) {
            words += word
        }

        override suspend fun remove(word: Word) {
            words -= word
        }

        override fun observe(sortOrder: BookmarksSortOrder, limit: Int): Flow<SavedWordsPage> = emptyFlow()

        override suspend fun remove(bookmark: SavedWord) = Unit

        override suspend fun restore(bookmark: SavedWord) = Unit
    }
}
