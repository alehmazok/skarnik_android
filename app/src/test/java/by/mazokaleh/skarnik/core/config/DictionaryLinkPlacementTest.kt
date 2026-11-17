package by.mazokaleh.skarnik.core.config

import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.FirebaseRemoteConfigValue
import kotlin.test.assertEquals
import org.junit.Test

class DictionaryLinkPlacementTest {
    @Test
    fun `before the first fetch the link is in the header`() {
        assertEquals(DictionaryLinkPlacement.Header, placement("", FirebaseRemoteConfig.VALUE_SOURCE_STATIC))
    }

    @Test
    fun `fetched values pick the variant`() {
        assertEquals(DictionaryLinkPlacement.Header, placement("header"))
        assertEquals(DictionaryLinkPlacement.Footer, placement("footer"))
        assertEquals(DictionaryLinkPlacement.None, placement("none"))
        assertEquals(DictionaryLinkPlacement.Footer, placement(" Footer "))
    }

    @Test
    fun `an empty or unknown value falls back to the header`() {
        assertEquals(DictionaryLinkPlacement.Header, placement(""))
        assertEquals(DictionaryLinkPlacement.Header, placement("bottom"))
    }

    private fun placement(raw: String, source: Int = FirebaseRemoteConfig.VALUE_SOURCE_REMOTE) =
        dictionaryLinkPlacementOf(FakeValue(raw, source))

    private class FakeValue(private val raw: String, private val source: Int) : FirebaseRemoteConfigValue {
        override fun asLong(): Long = raw.toLongOrNull() ?: throw IllegalArgumentException("not a long: $raw")

        override fun asDouble(): Double = raw.toDoubleOrNull() ?: throw IllegalArgumentException("not a double: $raw")

        override fun asString(): String = raw

        override fun asByteArray(): ByteArray = raw.toByteArray()

        override fun asBoolean(): Boolean = raw.toBooleanStrict()

        override fun getSource(): Int = source
    }
}
