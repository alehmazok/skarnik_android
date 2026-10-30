package by.mazokaleh.skarnik.feature.home.ui

import app.cash.turbine.test
import by.mazokaleh.skarnik.feature.home.data.DictionaryStatsRepository
import kotlin.test.assertEquals
import kotlinx.coroutines.CompletableDeferred
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
class HomeViewModelTest {
    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `shows the word count once the stores are open`() = runTest {
        val count = CompletableDeferred<Long>()
        val viewModel = HomeViewModel(FakeStatsRepository { count.await() })

        viewModel.uiState.test {
            assertEquals(HomeUiState.Loading, awaitItem())
            count.complete(42)
            assertEquals(HomeUiState.Ready(42), awaitItem())
        }
    }

    @Test
    fun `shows an error when the stores cannot be opened`() = runTest {
        val viewModel = HomeViewModel(FakeStatsRepository { error("corrupt store") })

        assertEquals(HomeUiState.Error, viewModel.uiState.value)
    }

    private class FakeStatsRepository(private val count: suspend () -> Long) : DictionaryStatsRepository {
        override suspend fun searchWordCount(): Long = count()
    }
}
