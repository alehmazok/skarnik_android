package by.mazokaleh.skarnik.core.analytics

import javax.inject.Inject
import javax.inject.Singleton

/**
 * Logs `screen_view` once each time a screen comes into view. A screen is identified by a key (its back stack
 * entry and tab), so it is logged again after the user leaves it and comes back, but not when it is merely
 * resumed again: after a rotation, or when the app returns from the background (as UIKit's tracking on iOS).
 * Main thread only.
 */
@Singleton
class ScreenViewTracker @Inject constructor(private val analytics: Analytics) {
    private val visible = mutableSetOf<String>()

    fun onShown(key: String, screen: Screen) {
        if (visible.add(key)) analytics.log(AnalyticsEvent.ScreenView(screen))
    }

    /** The user left the screen: the next [onShown] for [key] is a new screen view. */
    fun onLeft(key: String) {
        visible.remove(key)
    }
}
