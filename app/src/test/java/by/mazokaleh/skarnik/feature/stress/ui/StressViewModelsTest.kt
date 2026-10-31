package by.mazokaleh.skarnik.feature.stress.ui

import app.cash.turbine.test
import by.mazokaleh.skarnik.core.analytics.AnalyticsEvent
import by.mazokaleh.skarnik.core.analytics.FakeAnalytics
import by.mazokaleh.skarnik.feature.stress.domain.StressBackend
import by.mazokaleh.skarnik.feature.stress.domain.StressResolver
import by.mazokaleh.skarnik.feature.stress.domain.StressRow
import by.mazokaleh.skarnik.feature.stress.domain.StressSource
import by.mazokaleh.skarnik.feature.stress.domain.StressWordEntry
import java.io.IOException
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
class StressViewModelsTest {
    private val pair = listOf(
        StressWordEntry(1, "пара", "па́ра", "Nouns", StressSource.Starnik),
        StressWordEntry(2, "пара", "пара́", "Nouns", StressSource.Starnik)
    )
    private var words: List<StressWordEntry> = emptyList()
    private var rows: List<StressRow> = emptyList()
    private var failure: Throwable? = null
    private val backend = object : StressBackend {
        override suspend fun resolveWordList(word: String): List<StressWordEntry> {
            failure?.let { throw it }
            return words
        }

        override suspend fun stressTable(id: Long): List<StressRow> {
            failure?.let { throw it }
            return rows
        }
    }

    // The same fake behind both sources is enough here: the cascade itself is covered by StressResolverTest.
    private val resolver = StressResolver(backend, backend)
    private val analytics = FakeAnalytics()

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `several matches are listed`() = runTest {
        words = pair

        val viewModel = StressViewModel(StressRoute("пара"), resolver, analytics)

        assertEquals(StressUiState.Choose(pair), viewModel.uiState.value)
    }

    @Test
    fun `a single match opens its table`() = runTest {
        words = pair.take(1)

        val viewModel = StressViewModel(StressRoute("пара"), resolver, analytics)

        viewModel.openTable.test { assertEquals(pair.first(), awaitItem()) }
        assertEquals(StressUiState.Loading, viewModel.uiState.value)
    }

    @Test
    fun `opening the screen logs stress_clicked once, not on retry`() = runTest {
        failure = IOException("offline")
        val viewModel = StressViewModel(StressRoute("пара"), resolver, analytics)
        viewModel.retry()

        assertEquals(listOf<AnalyticsEvent>(AnalyticsEvent.StressClicked("пара")), analytics.events)
    }

    @Test
    fun `no match is not found`() = runTest {
        assertEquals(StressUiState.NotFound, StressViewModel(StressRoute("пара"), resolver, analytics).uiState.value)
    }

    @Test
    fun `an error can be retried`() = runTest {
        failure = IOException("offline")
        val viewModel = StressViewModel(StressRoute("пара"), resolver, analytics)
        assertEquals(StressUiState.Error, viewModel.uiState.value)

        failure = null
        words = pair
        viewModel.retry()

        assertEquals(StressUiState.Choose(pair), viewModel.uiState.value)
    }

    @Test
    fun `the table loads from the route's source`() = runTest {
        rows = listOf(StressRow("Форма", "па́ра"))

        val viewModel = StressTableViewModel(StressTableRoute(pair.first()), resolver)

        assertEquals(StressTableUiState.Loaded(rows), viewModel.uiState.value)
    }

    @Test
    fun `an empty table is not found`() = runTest {
        val viewModel = StressTableViewModel(StressTableRoute(pair.first()), resolver)

        assertEquals(StressTableUiState.NotFound, viewModel.uiState.value)
    }

    @Test
    fun `a failing table can be retried`() = runTest {
        failure = IOException("offline")
        val viewModel = StressTableViewModel(StressTableRoute(pair.first()), resolver)
        assertEquals(StressTableUiState.Error, viewModel.uiState.value)

        failure = null
        rows = listOf(StressRow("Форма", "па́ра"))
        viewModel.retry()

        assertEquals(StressTableUiState.Loaded(rows), viewModel.uiState.value)
    }
}
