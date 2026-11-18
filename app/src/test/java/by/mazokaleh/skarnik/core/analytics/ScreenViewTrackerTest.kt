package by.mazokaleh.skarnik.core.analytics

import by.mazokaleh.skarnik.core.analytics.AnalyticsEvent.ScreenView
import kotlin.test.assertEquals
import org.junit.Test

class ScreenViewTrackerTest {
    private val analytics = FakeAnalytics()
    private val tracker = ScreenViewTracker(analytics)

    @Test
    fun `a screen resumed again without being left is logged once`() {
        tracker.onShown("a/history", Screen.History)
        // Rotation or a return from the background.
        tracker.onShown("a/history", Screen.History)

        assertEquals(listOf<AnalyticsEvent>(ScreenView(Screen.History)), analytics.events)
    }

    @Test
    fun `coming back to a screen logs it again`() {
        tracker.onShown("a/history", Screen.History)
        tracker.onLeft("a/history")
        tracker.onShown("b/translation", Screen.Translation)
        tracker.onLeft("b/translation")
        tracker.onShown("a/history", Screen.History)

        assertEquals(
            listOf<AnalyticsEvent>(
                ScreenView(Screen.History),
                ScreenView(Screen.Translation),
                ScreenView(Screen.History)
            ),
            analytics.events
        )
    }

    @Test
    fun `a word opened beside the list is logged without leaving the list`() {
        tracker.onShown("a/history", Screen.History)
        tracker.onShown("p1/translation", Screen.Translation)
        tracker.onLeft("p1/translation")
        tracker.onShown("p2/translation", Screen.Translation)
        tracker.onShown("a/history", Screen.History)

        assertEquals(
            listOf<AnalyticsEvent>(
                ScreenView(Screen.History),
                ScreenView(Screen.Translation),
                ScreenView(Screen.Translation)
            ),
            analytics.events
        )
    }

    @Test
    fun `leaving a screen that was never shown is harmless`() {
        tracker.onLeft("a/history")
        tracker.onShown("a/history", Screen.History)

        assertEquals(listOf<AnalyticsEvent>(ScreenView(Screen.History)), analytics.events)
    }
}
