package by.mazokaleh.skarnik.core.config

import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.FirebaseRemoteConfigValue
import kotlin.test.assertEquals
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours
import org.junit.Test

class HttpCacheLifetimeTest {
    @Test
    fun `before the first fetch the lifetime is Flutter's default of 24 hours`() {
        assertEquals(24.hours, httpCacheLifetimeOf(FakeValue("", FirebaseRemoteConfig.VALUE_SOURCE_STATIC)))
    }

    @Test
    fun `a fetched value is a number of hours`() {
        assertEquals(6.hours, httpCacheLifetimeOf(FakeValue("6", FirebaseRemoteConfig.VALUE_SOURCE_REMOTE)))
    }

    @Test
    fun `zero hours from the console turns caching off`() {
        assertEquals(Duration.ZERO, httpCacheLifetimeOf(FakeValue("0", FirebaseRemoteConfig.VALUE_SOURCE_REMOTE)))
    }

    @Test
    fun `a negative or non-numeric value falls back to the default`() {
        assertEquals(24.hours, httpCacheLifetimeOf(FakeValue("-1", FirebaseRemoteConfig.VALUE_SOURCE_REMOTE)))
        assertEquals(24.hours, httpCacheLifetimeOf(FakeValue("day", FirebaseRemoteConfig.VALUE_SOURCE_REMOTE)))
    }

    /** Converts like Firebase's own value: a string that is not a number throws. */
    private class FakeValue(private val raw: String, private val source: Int) : FirebaseRemoteConfigValue {
        override fun asLong(): Long = raw.toLongOrNull() ?: throw IllegalArgumentException("not a long: $raw")

        override fun asDouble(): Double = raw.toDoubleOrNull() ?: throw IllegalArgumentException("not a double: $raw")

        override fun asString(): String = raw

        override fun asByteArray(): ByteArray = raw.toByteArray()

        override fun asBoolean(): Boolean = raw.toBooleanStrict()

        override fun getSource(): Int = source
    }
}
