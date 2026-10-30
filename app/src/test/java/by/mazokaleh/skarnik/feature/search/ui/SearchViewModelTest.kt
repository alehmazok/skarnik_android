package by.mazokaleh.skarnik.feature.search.ui

import app.cash.turbine.test
import by.mazokaleh.skarnik.feature.search.domain.FakeSearchIndex
import by.mazokaleh.skarnik.feature.search.domain.FakeSearchIndex.Companion.row
import by.mazokaleh.skarnik.feature.search.domain.SearchIndex
import by.mazokaleh.skarnik.feature.search.domain.SearchWord
import by.mazokaleh.skarnik.feature.search.domain.SearchWordsUseCase
import kotlin.test.assertEquals
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
class SearchViewModelTest {
    private val hello = row("прывітанне")
    private val index = FakeSearchIndex(listOf(hello, row("бывай")))

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun viewModel(searchIndex: SearchIndex = index) =
        SearchViewModel(SearchWordsUseCase(searchIndex, Dispatchers.Unconfined))

    @Test
    fun `shows the hint, then results for a query`() = runTest {
        val viewModel = viewModel()

        viewModel.uiState.test {
            assertEquals(SearchUiState.Hint, awaitItem())
            viewModel.onQueryChange("  прыв ")
            assertEquals(SearchUiState.Results("прыв", listOf(hello.word)), awaitItem())
        }
    }

    @Test
    fun `says when nothing is found`() = runTest {
        val viewModel = viewModel()

        viewModel.uiState.test {
            awaitItem()
            viewModel.onQueryChange("ыыы")
            assertEquals(SearchUiState.NothingFound("ыыы"), awaitItem())
        }
    }

    @Test
    fun `goes back to the hint when the query is cleared, without searching blanks`() = runTest {
        val viewModel = viewModel()

        viewModel.uiState.test {
            awaitItem()
            viewModel.onQueryChange("прыв")
            awaitItem()
            viewModel.onQueryChange("   ")
            assertEquals(SearchUiState.Hint, awaitItem())
        }
        assertEquals(1, index.prefixQueries.size)
    }

    @Test
    fun `searches only the last of quickly typed queries`() = runTest {
        val viewModel = viewModel()

        viewModel.uiState.test {
            awaitItem()
            viewModel.onQueryChange("п")
            viewModel.onQueryChange("пр")
            viewModel.onQueryChange("пры")
            assertEquals(SearchUiState.Results("пры", listOf(hello.word)), awaitItem())
        }
        assertEquals(listOf(setOf("пры")), index.prefixQueries)
    }

    @Test
    fun `shows an error when the index fails`() = runTest {
        val viewModel = viewModel(BrokenSearchIndex)

        viewModel.uiState.test {
            awaitItem()
            viewModel.onQueryChange("прыв")
            assertEquals(SearchUiState.Error, awaitItem())
        }
    }

    private object BrokenSearchIndex : SearchIndex {
        override suspend fun findByWord(prefixes: Set<String>, limit: Int): List<SearchWord> = error("corrupt store")

        override suspend fun findByWordMask(prefixes: Set<String>, excludedIds: Set<Long>, limit: Int) =
            emptyList<SearchWord>()

        override suspend fun findByLetter(letter: String, limit: Int) = emptyList<SearchWord>()
    }
}
