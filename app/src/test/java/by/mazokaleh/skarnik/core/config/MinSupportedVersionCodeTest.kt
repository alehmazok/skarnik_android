package by.mazokaleh.skarnik.core.config

import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import kotlin.test.assertEquals
import org.junit.Test

class MinSupportedVersionCodeTest {
    @Test
    fun `before the first fetch nothing is blocked`() {
        val static = FakeRemoteConfigValue("", FirebaseRemoteConfig.VALUE_SOURCE_STATIC)
        assertEquals(0L, minSupportedVersionCodeOf(static))
    }

    @Test
    fun `a fetched number is the minimum`() {
        assertEquals(171L, minSupportedVersionCodeOf(FakeRemoteConfigValue("171")))
        assertEquals(171L, minSupportedVersionCodeOf(FakeRemoteConfigValue(" 171 ")))
    }

    @Test
    fun `an empty, negative or non-numeric value blocks nothing`() {
        assertEquals(0L, minSupportedVersionCodeOf(FakeRemoteConfigValue("")))
        assertEquals(0L, minSupportedVersionCodeOf(FakeRemoteConfigValue("-5")))
        assertEquals(0L, minSupportedVersionCodeOf(FakeRemoteConfigValue("4.1.0")))
    }
}
