package by.mazokaleh.skarnik.feature.translation.ui

import by.mazokaleh.skarnik.core.analytics.AnalyticsEvent
import by.mazokaleh.skarnik.core.analytics.FakeAnalytics
import by.mazokaleh.skarnik.feature.translation.domain.WordLinkHintSettings
import java.io.IOException
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class WordLinkHintViewModelTest {
    private val analytics = FakeAnalytics()
    private var failing = false
    private val settings = object : WordLinkHintSettings {
        var shown = false

        override suspend fun isHintShown(): Boolean {
            if (failing) throw IOException("disk")
            return shown
        }

        override suspend fun markHintShown() {
            shown = true
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
    fun `shows the hint once and saves it as shown right away`() {
        val first = WordLinkHintViewModel(settings, analytics)

        first.onWordLinksShown()

        assertTrue(first.visible.value)
        assertTrue(settings.shown)
        assertEquals(listOf<AnalyticsEvent>(AnalyticsEvent.WordLinkHintShown), analytics.events)

        val next = WordLinkHintViewModel(settings, analytics)
        next.onWordLinksShown()
        assertFalse(next.visible.value)
    }

    @Test
    fun `a tap on a word ends the hint, logged once`() {
        val viewModel = WordLinkHintViewModel(settings, analytics)
        viewModel.onWordLinksShown()

        viewModel.onHintDone(byTap = true)
        viewModel.onHintDone(byTap = false)

        assertFalse(viewModel.visible.value)
        assertEquals(
            listOf(AnalyticsEvent.WordLinkHintShown, AnalyticsEvent.WordLinkHintDismissed(byTap = true)),
            analytics.events
        )
    }

    @Test
    fun `no hint when the flag cannot be read`() {
        failing = true
        val viewModel = WordLinkHintViewModel(settings, analytics)

        viewModel.onWordLinksShown()

        assertFalse(viewModel.visible.value)
        assertEquals(emptyList(), analytics.events)
    }
}
