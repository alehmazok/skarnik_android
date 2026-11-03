package by.mazokaleh.skarnik.baselineprofile

import androidx.benchmark.macro.MacrobenchmarkScope
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Direction
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.Until
import java.util.regex.Pattern

/**
 * The user journeys the profile covers and the benchmarks measure. Elements are found by their Compose test tags,
 * which MainActivity exposes as resource ids (the same ids the Maestro flows use).
 */

/** The app under test, passed by the module's build script (by.mazokaleh.skarnik for the release variants). */
val targetAppId: String
    get() = requireNotNull(InstrumentationRegistry.getArguments().getString("targetAppId")) {
        "targetAppId instrumentation argument is missing"
    }

/** The first launch unpacks the 60 MB search index, which takes a while on a CI emulator. */
private const val FIRST_LAUNCH_TIMEOUT_MS = 60_000L
private const val SCREEN_TIMEOUT_MS = 10_000L
private const val CONSENT_TIMEOUT_MS = 5_000L
private const val NETWORK_TIMEOUT_MS = 15_000L
private const val FLINGS = 3

/** A fifth of the screen width on each side, clear of the system back gesture and the letter index. */
private const val GESTURE_MARGIN_DIVISOR = 5

/** Belarusian «Не дазваляць»: the consent dialog is a popup window without resource ids, so it is found by text. */
private const val DECLINE_ANALYTICS = "Не дазваляць"

/** The History tab, empty or not: the start screen is ready. */
private val historyReady = By.res(Pattern.compile("history_(empty|list)"))

/**
 * Answers the consent dialog a fresh install shows (declined, so a benchmark never sends analytics) and waits for
 * the History tab. Later launches have no dialog and only wait.
 */
fun UiDevice.dismissConsentAndWaitForHistory() {
    wait(Until.findObject(By.text(DECLINE_ANALYTICS)), CONSENT_TIMEOUT_MS)?.click()
    checkNotNull(wait(Until.findObject(historyReady), FIRST_LAUNCH_TIMEOUT_MS)) { "History tab did not show" }
}

/** Launches the app with nothing on screen yet but its process, so the launch is cold, then waits for History. */
fun prepareFirstLaunch(packageName: String) {
    val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
    device.executeShellCommand("am start -W -n $packageName/by.mazokaleh.skarnik.MainActivity")
    device.dismissConsentAndWaitForHistory()
    device.executeShellCommand("am force-stop $packageName")
}

/** Cold start to the History tab. */
fun MacrobenchmarkScope.startToHistory() {
    pressHome()
    startActivityAndWait()
    device.dismissConsentAndWaitForHistory()
}

/** Opens search, types a word with a Russian letter (so the letter substitution runs) and opens the first result. */
fun MacrobenchmarkScope.searchAndOpenWord() {
    device.findObject(By.res("home_search")).click()
    val field = checkNotNull(device.wait(Until.findObject(By.res("search_field")), SCREEN_TIMEOUT_MS)) {
        "Search field did not show"
    }
    field.text = "прывитанне"
    val result = checkNotNull(device.wait(Until.findObject(By.res("search_result")), SCREEN_TIMEOUT_MS)) {
        "No search results"
    }
    result.click()
    checkNotNull(device.wait(Until.findObject(By.res("translation_word")), SCREEN_TIMEOUT_MS)) {
        "Translation screen did not show"
    }
    // The translation comes from the network; render it when it arrives, but a slow network does not fail the run.
    device.wait(Until.hasObject(By.res("translation_text")), NETWORK_TIMEOUT_MS)
    device.pressBack()
    device.pressBack()
    device.wait(Until.findObject(historyReady), SCREEN_TIMEOUT_MS)
}

/** Opens the Dictionary tab (the whole explanatory dictionary) and scrolls it, then shows Bookmarks and History. */
fun MacrobenchmarkScope.browseDictionary() {
    device.findObject(By.res("home_tab_dictionary")).click()
    val list = checkNotNull(device.wait(Until.findObject(By.res("vocabulary_list")), SCREEN_TIMEOUT_MS)) {
        "Dictionary list did not show"
    }
    list.setGestureMargin(device.displayWidth / GESTURE_MARGIN_DIVISOR)
    repeat(FLINGS) {
        list.fling(Direction.DOWN)
        device.waitForIdle()
    }
    device.findObject(By.res("home_tab_bookmarks")).click()
    device.wait(Until.findObject(By.res(Pattern.compile("bookmarks_(empty|list)"))), SCREEN_TIMEOUT_MS)
    device.findObject(By.res("home_tab_history")).click()
    device.wait(Until.findObject(historyReady), SCREEN_TIMEOUT_MS)
}
