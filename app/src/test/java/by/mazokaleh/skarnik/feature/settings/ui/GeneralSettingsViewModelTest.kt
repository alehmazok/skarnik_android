package by.mazokaleh.skarnik.feature.settings.ui

import app.cash.turbine.test
import by.mazokaleh.skarnik.core.analytics.ConsentState
import by.mazokaleh.skarnik.core.analytics.FakeAnalyticsConsent
import by.mazokaleh.skarnik.core.model.Dictionary
import by.mazokaleh.skarnik.core.model.SavedWord
import by.mazokaleh.skarnik.core.model.SavedWordsPage
import by.mazokaleh.skarnik.core.model.Word
import by.mazokaleh.skarnik.core.settings.Appearance
import by.mazokaleh.skarnik.core.settings.AppearanceSettings
import by.mazokaleh.skarnik.core.settings.ThemeMode
import by.mazokaleh.skarnik.feature.history.domain.HistoryRepository
import java.io.IOException
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class GeneralSettingsViewModelTest {
    private val appearance = object : AppearanceSettings {
        val stored = MutableStateFlow(Appearance())
        override val appearance: Flow<Appearance> = stored

        override suspend fun setThemeMode(themeMode: ThemeMode) = stored.update { it.copy(themeMode = themeMode) }

        override suspend fun setDynamicColor(enabled: Boolean) = stored.update { it.copy(dynamicColor = enabled) }
    }

    private val historyWords = MutableStateFlow<List<SavedWord>>(emptyList())
    private var failClear = false
    private val history = object : HistoryRepository {
        override suspend fun save(word: Word) = Unit

        override suspend fun clear() {
            if (failClear) throw IOException("disk full")
            historyWords.value = emptyList()
        }

        override fun observe(limit: Int): Flow<SavedWordsPage> =
            historyWords.map { SavedWordsPage(it.take(limit), hasMore = it.size > limit) }
    }

    private val consent = FakeAnalyticsConsent()

    private fun viewModel() = GeneralSettingsViewModel(appearance, history, consent)

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `theme and colour changes are saved and shown`() = runTest {
        val viewModel = viewModel()
        viewModel.state.test {
            assertEquals(Appearance(), awaitItem().appearance)
            viewModel.setThemeMode(ThemeMode.Light)
            assertEquals(ThemeMode.Light, awaitItem().appearance.themeMode)
            viewModel.setDynamicColor(false)
            assertFalse(awaitItem().appearance.dynamicColor)
        }
        assertEquals(Appearance(ThemeMode.Light, dynamicColor = false), appearance.stored.value)
    }

    @Test
    fun `the analytics switch answers the consent question`() = runTest {
        val viewModel = viewModel()
        viewModel.state.test {
            assertFalse(awaitItem().analyticsEnabled)
            viewModel.setAnalyticsEnabled(true)
            assertTrue(awaitItem().analyticsEnabled)
        }
        assertEquals(ConsentState(answered = true, granted = true), consent.stored.value)
    }

    @Test
    fun `clearing history empties it, disables the row and confirms`() = runTest {
        historyWords.value = listOf(SavedWord(1, Dictionary.BelRus, 10, "хата", "х"))
        val viewModel = viewModel()
        viewModel.state.test {
            // The initial empty state may or may not be seen before the store's.
            if (!awaitItem().hasHistory) assertTrue(awaitItem().hasHistory)
            viewModel.events.test {
                viewModel.clearHistory()
                assertEquals(SettingsEvent.HistoryCleared, awaitItem())
            }
            assertFalse(awaitItem().hasHistory)
        }
    }

    @Test
    fun `a failed clear says so and keeps the history`() = runTest {
        historyWords.value = listOf(SavedWord(1, Dictionary.BelRus, 10, "хата", "х"))
        failClear = true
        val viewModel = viewModel()
        viewModel.events.test {
            viewModel.clearHistory()
            assertEquals(SettingsEvent.HistoryClearFailed, awaitItem())
        }
        assertEquals(1, historyWords.value.size)
    }
}
