package by.mazokaleh.skarnik.feature.settings.ui

import kotlin.test.assertEquals
import org.junit.Test

class SupportFooterTest {
    private fun footer(manufacturer: String, model: String) = ExternalActions.supportFooter(
        appName = "Skarnik",
        versionName = "4.0.2",
        versionCode = 169,
        androidRelease = "15",
        sdkInt = 35,
        manufacturer = manufacturer,
        model = model
    )

    @Test
    fun `footer has app version, Android version and device`() {
        assertEquals(
            "—\nSkarnik 4.0.2 (169)\nAndroid 15 (API 35) · Samsung SM-S918B",
            footer(manufacturer = "samsung", model = "SM-S918B")
        )
    }

    @Test
    fun `manufacturer is not repeated when the model starts with it`() {
        assertEquals(
            "—\nSkarnik 4.0.2 (169)\nAndroid 15 (API 35) · Nokia 7.2",
            footer(manufacturer = "Nokia", model = "Nokia 7.2")
        )
    }
}
