package by.mazokaleh.skarnik.navigation

import android.app.Activity
import androidx.activity.ComponentActivity
import androidx.activity.compose.LocalActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.navigation.NavBackStackEntry
import by.mazokaleh.skarnik.core.analytics.Screen
import by.mazokaleh.skarnik.core.analytics.ScreenViewTracker

/** Provided by MainActivity; null in previews, where nothing is logged. */
val LocalScreenViewTracker = staticCompositionLocalOf<ScreenViewTracker?> { null }

/**
 * Logs [screen] as a `screen_view` when this destination (or tab) is resumed, through [ScreenViewTracker].
 *
 * The screen is left when it pauses while the activity stays resumed (another destination covers it) or when it
 * leaves the composition (a tab switch, back); a pause of the whole activity (background) or a recreation
 * (rotation, theme change) keeps it shown, so neither logs it twice.
 */
@Composable
fun TrackScreenView(screen: Screen) {
    val tracker = LocalScreenViewTracker.current ?: return
    val owner = LocalLifecycleOwner.current
    val activity = LocalActivity.current
    // The back stack entry id survives recreation, so the key does too.
    val key = "${(owner as? NavBackStackEntry)?.id}/${screen.screenName}"
    DisposableEffect(tracker, owner, key) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> tracker.onShown(key, screen)
                Lifecycle.Event.ON_PAUSE -> if (activity.hasResumedLifecycle()) tracker.onLeft(key)
                else -> Unit
            }
        }
        owner.lifecycle.addObserver(observer)
        onDispose {
            owner.lifecycle.removeObserver(observer)
            if (activity?.isChangingConfigurations != true) tracker.onLeft(key)
        }
    }
}

/** While the activity itself pauses, its lifecycle is already below RESUMED when the screens hear ON_PAUSE. */
private fun Activity?.hasResumedLifecycle(): Boolean =
    (this as? ComponentActivity)?.lifecycle?.currentState?.isAtLeast(Lifecycle.State.RESUMED) == true
