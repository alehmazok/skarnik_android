package by.mazokaleh.skarnik.feature.settings.ui

import app.cash.turbine.ReceiveTurbine
import app.cash.turbine.test
import by.mazokaleh.skarnik.core.analytics.AnalyticsEvent
import by.mazokaleh.skarnik.core.analytics.FakeAnalytics
import by.mazokaleh.skarnik.core.model.Dictionary
import by.mazokaleh.skarnik.feature.offline.domain.CheckDownloadRateLimitUseCase
import by.mazokaleh.skarnik.feature.offline.domain.DeleteOfflineDictionaryUseCase
import by.mazokaleh.skarnik.feature.offline.domain.DownloadJob
import by.mazokaleh.skarnik.feature.offline.domain.DownloadProgress
import by.mazokaleh.skarnik.feature.offline.domain.FakeOfflineDictionarySettings
import by.mazokaleh.skarnik.feature.offline.domain.FakeOfflineDictionaryStore
import by.mazokaleh.skarnik.feature.offline.domain.OfflineDictionaryStatus
import by.mazokaleh.skarnik.feature.offline.domain.OfflineDownloads
import by.mazokaleh.skarnik.feature.offline.domain.downloadedWord
import kotlin.test.assertEquals
import kotlin.test.assertTrue
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
class SettingsViewModelTest {
    private val settings = FakeOfflineDictionarySettings()
    private val store = FakeOfflineDictionaryStore()
    private val downloads = object : OfflineDownloads {
        val jobs = MutableStateFlow<Map<Dictionary, DownloadJob>>(emptyMap())
        val started = mutableListOf<Dictionary>()

        override fun start(dictionary: Dictionary) {
            started += dictionary
            jobs.value += dictionary to DownloadJob.Active(null)
        }

        override fun observe(dictionary: Dictionary): Flow<DownloadJob?> = jobs.map { it[dictionary] }
    }
    private var now = 1_000_000L
    private val analytics = FakeAnalytics()

    private fun viewModel() = SettingsViewModel(
        downloads = downloads,
        settings = settings,
        checkRateLimit = CheckDownloadRateLimitUseCase(settings) { now },
        deleteDictionary = DeleteOfflineDictionaryUseCase(store, settings),
        analytics = analytics,
        store = store
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private suspend fun ReceiveTurbine<OfflineSectionState>.awaitReady(): OfflineSectionState.Ready {
        var state = awaitItem()
        while (state == OfflineSectionState.Loading) state = awaitItem()
        return state as OfflineSectionState.Ready
    }

    private fun OfflineSectionState.statuses() =
        (this as OfflineSectionState.Ready).items.associate { it.dictionary to it.status }

    @Test
    fun `lists the three dictionaries in Flutter's order with their state`() = runTest {
        store.putAll(Dictionary.RusBel, listOf(downloadedWord(1), downloadedWord(2)))
        downloads.jobs.value = mapOf(Dictionary.Tsbm to DownloadJob.Active(DownloadProgress(5, 50)))

        viewModel().offline.test {
            val state = awaitReady()
            val order = listOf(Dictionary.BelRus, Dictionary.RusBel, Dictionary.Tsbm)
            assertEquals(order, state.items.map { it.dictionary })
            assertEquals(
                mapOf(
                    Dictionary.BelRus to OfflineDictionaryStatus.NotDownloaded,
                    Dictionary.RusBel to OfflineDictionaryStatus.Downloaded(2),
                    Dictionary.Tsbm to OfflineDictionaryStatus.Downloading(DownloadProgress(5, 50))
                ),
                state.statuses()
            )
        }
    }

    @Test
    fun `download starts the job and the row follows it`() = runTest {
        val viewModel = viewModel()
        viewModel.offline.test {
            awaitReady()
            viewModel.download(Dictionary.BelRus)
            assertEquals(OfflineDictionaryStatus.Downloading(null), awaitItem().statuses()[Dictionary.BelRus])
            assertEquals(listOf(Dictionary.BelRus), downloads.started)
        }
        assertEquals(listOf<AnalyticsEvent>(AnalyticsEvent.OfflineDictionaryDownloadClicked(Dictionary.BelRus)), analytics.events)
    }

    @Test
    fun `a running or finished download is not started again`() = runTest {
        store.putAll(Dictionary.RusBel, listOf(downloadedWord(1)))
        downloads.jobs.value = mapOf(Dictionary.Tsbm to DownloadJob.Active(null))
        val viewModel = viewModel()
        viewModel.offline.test {
            awaitReady()
            viewModel.download(Dictionary.Tsbm)
            viewModel.download(Dictionary.RusBel)
            assertTrue(downloads.started.isEmpty())
            assertTrue(settings.attempts.isEmpty())
        }
    }

    @Test
    fun `the fifth start within five minutes is refused with a message`() = runTest {
        settings.attempts = List(CheckDownloadRateLimitUseCase.MAX_ATTEMPTS) { now - it }
        val viewModel = viewModel()
        viewModel.events.test {
            viewModel.download(Dictionary.BelRus)
            assertEquals(SettingsEvent.RateLimited, awaitItem())
            assertTrue(downloads.started.isEmpty())
        }
    }

    @Test
    fun `delete removes the words and the cursor`() = runTest {
        store.putAll(Dictionary.Tsbm, listOf(downloadedWord(1)))
        settings.setCursor(Dictionary.Tsbm, 1)
        val viewModel = viewModel()
        viewModel.events.test {
            viewModel.delete(Dictionary.Tsbm)
            assertEquals(SettingsEvent.Deleted, awaitItem())
        }
        assertEquals(0, store.count(Dictionary.Tsbm))
        assertEquals(null, settings.cursor(Dictionary.Tsbm).first())
    }

    @Test
    fun `opening Settings clears the badge`() = runTest {
        val badge = SettingsBadgeViewModel(settings)
        badge.showBadge.test {
            // The initial false may or may not be seen before the stored value arrives.
            if (!awaitItem()) assertEquals(true, awaitItem())
            viewModel()
            assertEquals(false, awaitItem())
        }
    }
}
