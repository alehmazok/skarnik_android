package by.mazokaleh.skarnik.baselineprofile

import androidx.benchmark.macro.junit4.BaselineProfileRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Generates the Baseline Profile and the startup profile: `./gradlew :app:generateReleaseBaselineProfile` with a
 * device or emulator connected (Android 13+). The result lands in app/src/release/generated/baselineProfiles/.
 */
@RunWith(AndroidJUnit4::class)
class BaselineProfileGenerator {
    @get:Rule
    val rule = BaselineProfileRule()

    @Test
    fun generate() = rule.collect(packageName = targetAppId, includeInStartupProfile = true) {
        startToHistory()
        searchAndOpenWord()
        browseDictionary()
    }
}
