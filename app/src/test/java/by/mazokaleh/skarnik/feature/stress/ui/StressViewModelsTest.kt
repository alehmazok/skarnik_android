package by.mazokaleh.skarnik.feature.stress.ui

import app.cash.turbine.test
import by.mazokaleh.skarnik.core.analytics.AnalyticsEvent
import by.mazokaleh.skarnik.core.analytics.FakeAnalytics
import by.mazokaleh.skarnik.feature.stress.domain.StressBackend
import by.mazokaleh.skarnik.feature.stress.domain.StressResolver
import by.mazokaleh.skarnik.feature.stress.domain.StressRow
import by.mazokaleh.skarnik.feature.stress.domain.StressSource
import by.mazokaleh.skarnik.feature.stress.domain.StressWordEntry
import by.mazokaleh.skarnik.feature.stress.domain.stressPickerSections
import java.io.IOException
import kotlin.test.assertEquals
import kotlin.test.assertNull
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

    private fun lookup() = StressLookupViewModel(resolver, analytics)

    @Test
    fun `several matches open the picker`() = runTest {
        words = pair

        val viewModel = lookup()
        viewModel.lookup("пара")

        assertEquals(StressPicker("пара", stressPickerSections(pair)), viewModel.uiState.value.picker)
        assertNull(viewModel.uiState.value.loadingWord)
    }

    @Test
    fun `a single match opens its table`() = runTest {
        words = pair.take(1)

        val viewModel = lookup()
        viewModel.events.test {
            viewModel.lookup("пара")
            assertEquals(StressLookupEvent.OpenTable(pair.first()), awaitItem())
        }
        assertNull(viewModel.uiState.value.picker)
    }

    @Test
    fun `picking a row closes the sheet and opens one table`() = runTest {
        words = pair
        val viewModel = lookup()
        viewModel.lookup("пара")

        viewModel.events.test {
            viewModel.choose(pair[1])
            viewModel.choose(pair[0])
            assertEquals(StressLookupEvent.OpenTable(pair[1]), awaitItem())
            expectNoEvents()
        }
        assertNull(viewModel.uiState.value.picker)
    }

    @Test
    fun `dismissing the sheet opens nothing`() = runTest {
        words = pair
        val viewModel = lookup()
        viewModel.lookup("пара")

        viewModel.events.test {
            viewModel.dismissPicker()
            expectNoEvents()
        }
        assertNull(viewModel.uiState.value.picker)
    }

    @Test
    fun `a lookup logs stress_clicked, a retry does not`() = runTest {
        failure = IOException("offline")
        val viewModel = lookup()
        viewModel.lookup("пара")
        viewModel.retry("пара")

        assertEquals(listOf<AnalyticsEvent>(AnalyticsEvent.StressClicked("пара")), analytics.events)
    }

    @Test
    fun `no match is not found`() = runTest {
        val viewModel = lookup()
        viewModel.events.test {
            viewModel.lookup("пара")
            assertEquals(StressLookupEvent.NotFound, awaitItem())
        }
    }

    @Test
    fun `an error can be retried`() = runTest {
        failure = IOException("offline")
        val viewModel = lookup()
        viewModel.events.test {
            viewModel.lookup("пара")
            assertEquals(StressLookupEvent.Failed("пара"), awaitItem())
        }

        failure = null
        words = pair
        viewModel.retry("пара")

        assertEquals(stressPickerSections(pair), viewModel.uiState.value.picker?.sections)
    }

    @Test
    fun `the chip shows progress while the word is looked up`() = runTest {
        val gate = CompletableDeferred<Unit>()
        val slow = object : StressBackend {
            override suspend fun resolveWordList(word: String): List<StressWordEntry> {
                gate.await()
                return pair
            }

            override suspend fun stressTable(id: Long): List<StressRow> = emptyList()
        }
        val viewModel = StressLookupViewModel(StressResolver(slow, slow), analytics)

        viewModel.lookup("пара")
        viewModel.lookup("пара")
        assertEquals("пара", viewModel.uiState.value.loadingWord)

        gate.complete(Unit)
        assertNull(viewModel.uiState.value.loadingWord)
        assertEquals(1, analytics.events.size)
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

    @Test
    fun `the table route carries the full meaning`() {
        val entry = pair.first().copy(meaning = "два аднолькавыя прадметы")

        assertEquals("два аднолькавыя прадметы", StressTableRoute(entry).meaning)
    }

    @Test
    fun `no meanings is flagged for the picker`() {
        assertEquals(true, StressPicker("пара", stressPickerSections(pair)).noMeanings)
        val withMeaning = listOf(pair[0].copy(meaning = "адно"), pair[1])
        assertEquals(false, StressPicker("пара", stressPickerSections(withMeaning)).noMeanings)
    }
}
