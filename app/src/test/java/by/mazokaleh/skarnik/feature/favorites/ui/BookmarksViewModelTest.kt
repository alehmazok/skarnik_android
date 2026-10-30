package by.mazokaleh.skarnik.feature.favorites.ui

import app.cash.turbine.ReceiveTurbine
import app.cash.turbine.test
import by.mazokaleh.skarnik.core.model.Dictionary
import by.mazokaleh.skarnik.core.model.SAVED_WORDS_PAGE_SIZE
import by.mazokaleh.skarnik.core.model.SavedWord
import by.mazokaleh.skarnik.core.model.SavedWordsPage
import by.mazokaleh.skarnik.core.model.Word
import by.mazokaleh.skarnik.feature.favorites.domain.BookmarksRepository
import by.mazokaleh.skarnik.feature.favorites.domain.BookmarksSortOrder
import by.mazokaleh.skarnik.feature.favorites.domain.BookmarksSortRepository
import kotlin.test.assertEquals
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class BookmarksViewModelTest {
    private val bye = SavedWord(1, Dictionary.BelRus, 11, "бывай", "б")
    private val hello = SavedWord(2, Dictionary.BelRus, 10, "прывітанне", "п")
    private val bookmarks = FakeBookmarks(listOf(bye, hello))
    private val sort = FakeSort()

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `lists bookmarks in the remembered order and re-sorts on change`() = runTest {
        sort.order.value = BookmarksSortOrder.Alphabetical
        val viewModel = BookmarksViewModel(bookmarks, sort)

        viewModel.uiState.test {
            assertEquals(
                BookmarksUiState.Ready(listOf(bye, hello), hasMore = false, BookmarksSortOrder.Alphabetical),
                awaitLoaded()
            )
            viewModel.setSortOrder(BookmarksSortOrder.DateAdded)
            assertEquals(
                BookmarksUiState.Ready(listOf(hello, bye), hasMore = false, BookmarksSortOrder.DateAdded),
                awaitItem()
            )
        }
        assertEquals(BookmarksSortOrder.DateAdded, sort.order.value)
    }

    @Test
    fun `a new sort order starts from the first page`() = runTest {
        bookmarks.rows.value = (1..SAVED_WORDS_PAGE_SIZE * 2).map {
            SavedWord(it.toLong(), Dictionary.Tsbm, it.toLong(), "w$it", "w")
        }
        val viewModel = BookmarksViewModel(bookmarks, sort)

        viewModel.uiState.test {
            awaitLoaded()
            viewModel.loadMore()
            assertEquals(SAVED_WORDS_PAGE_SIZE * 2, (awaitItem() as BookmarksUiState.Ready).words.size)
            viewModel.setSortOrder(BookmarksSortOrder.Alphabetical)
            assertEquals(SAVED_WORDS_PAGE_SIZE, (awaitLoaded() as BookmarksUiState.Ready).words.size)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `removing announces the removal and undo puts the bookmark back`() = runTest {
        val viewModel = BookmarksViewModel(bookmarks, sort)

        viewModel.remove(hello)

        assertEquals(BookmarksEvent.Removed(hello), viewModel.events.first())
        assertEquals(listOf(bye), bookmarks.rows.value)

        viewModel.undoRemove(hello)

        assertEquals(listOf(bye, hello), bookmarks.rows.value)
    }

    private suspend fun ReceiveTurbine<BookmarksUiState>.awaitLoaded(): BookmarksUiState {
        var item = awaitItem()
        while (item == BookmarksUiState.Loading) item = awaitItem()
        return item
    }

    private class FakeSort : BookmarksSortRepository {
        val order = MutableStateFlow(BookmarksSortOrder.DateAdded)
        override val sortOrder: Flow<BookmarksSortOrder> = order

        override suspend fun setSortOrder(sortOrder: BookmarksSortOrder) {
            order.value = sortOrder
        }
    }

    /** Rows kept in id order; date order is newest (highest id) first. */
    private class FakeBookmarks(initial: List<SavedWord>) : BookmarksRepository {
        val rows = MutableStateFlow(initial)

        override suspend fun isBookmarked(word: Word) = false

        override suspend fun add(word: Word) = Unit

        override suspend fun remove(word: Word) = Unit

        override fun observe(sortOrder: BookmarksSortOrder, limit: Int): Flow<SavedWordsPage> = rows.map { rows ->
            val sorted = when (sortOrder) {
                BookmarksSortOrder.DateAdded -> rows.sortedByDescending { it.id }
                BookmarksSortOrder.Alphabetical -> rows.sortedBy { it.word }
            }
            SavedWordsPage(sorted.take(limit), hasMore = sorted.size > limit)
        }

        override suspend fun remove(bookmark: SavedWord) {
            rows.value -= bookmark
        }

        override suspend fun restore(bookmark: SavedWord) {
            rows.value = (rows.value + bookmark).sortedBy { it.id }
        }
    }
}
