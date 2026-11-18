package by.mazokaleh.skarnik.core.config

import com.google.firebase.remoteconfig.FirebaseRemoteConfig
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
        dictionaryLinkPlacementOf(FakeRemoteConfigValue(raw, source))
}
