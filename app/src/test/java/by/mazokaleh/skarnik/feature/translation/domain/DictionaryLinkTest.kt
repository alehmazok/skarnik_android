package by.mazokaleh.skarnik.feature.translation.domain

import by.mazokaleh.skarnik.core.config.DictionaryLinkPlacement
import by.mazokaleh.skarnik.core.config.FakeRemoteConfig
import by.mazokaleh.skarnik.core.model.Dictionary
import by.mazokaleh.skarnik.core.model.Word
import by.mazokaleh.skarnik.core.model.WordLink
import java.io.IOException
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlinx.coroutines.test.runTest
import org.junit.Test

class DictionaryLinkTest {
    private val asked = mutableListOf<Pair<String, Dictionary>>()
    private val index = SameWordIndex { lword, dictionary ->
        asked += lword to dictionary
        WordLink(dictionary, lword.length.toLong())
    }
    private val remoteConfig = FakeRemoteConfig()

    private fun word(dictionary: Dictionary) = Word(dictionary, 1, "Бурштын", "б", "бурштын", null)

    @Test
    fun `Бел-Рус links to ТСБМ and ТСБМ back to Бел-Рус by lword`() = runTest {
        val find = FindDictionaryLinkUseCase(index, remoteConfig)

        assertEquals(
            DictionaryLink(WordLink(Dictionary.Tsbm, 7), DictionaryLinkPlacement.Header),
            find(word(Dictionary.BelRus))
        )
        assertEquals(
            DictionaryLink(WordLink(Dictionary.BelRus, 7), DictionaryLinkPlacement.Header),
            find(word(Dictionary.Tsbm))
        )
        assertEquals(listOf("бурштын" to Dictionary.Tsbm, "бурштын" to Dictionary.BelRus), asked)
    }

    @Test
    fun `Рус-Бел has no linked dictionary`() = runTest {
        assertNull(FindDictionaryLinkUseCase(index, remoteConfig)(word(Dictionary.RusBel)))
        assertEquals(emptyList(), asked)
    }

    @Test
    fun `the placement comes from Remote Config and none skips the lookup`() = runTest {
        val find = FindDictionaryLinkUseCase(index, remoteConfig)
        remoteConfig.dictionaryLinkPlacement = DictionaryLinkPlacement.Footer
        assertEquals(DictionaryLinkPlacement.Footer, find(word(Dictionary.BelRus))?.placement)

        remoteConfig.dictionaryLinkPlacement = DictionaryLinkPlacement.None
        asked.clear()
        assertNull(find(word(Dictionary.BelRus)))
        assertEquals(emptyList(), asked)
    }

    @Test
    fun `no entry or an index error means no link`() = runTest {
        assertNull(FindDictionaryLinkUseCase({ _, _ -> null }, remoteConfig)(word(Dictionary.BelRus)))
        val broken = FindDictionaryLinkUseCase({ _, _ -> throw IOException("disk") }, remoteConfig)
        assertNull(broken(word(Dictionary.BelRus)))
    }
}
