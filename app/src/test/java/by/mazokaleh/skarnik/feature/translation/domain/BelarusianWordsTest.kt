package by.mazokaleh.skarnik.feature.translation.domain

import by.mazokaleh.skarnik.core.model.Dictionary
import by.mazokaleh.skarnik.core.model.Word
import by.mazokaleh.skarnik.core.model.WordLink
import java.io.IOException
import kotlin.test.assertEquals
import kotlinx.coroutines.test.runTest
import org.junit.Test

class BelarusianWordsTest {
    private val index = mapOf(
        "янтар" to WordLink(Dictionary.BelRus, 10),
        "бурштын" to WordLink(Dictionary.BelRus, 27890),
        "лодар" to WordLink(Dictionary.Tsbm, 5)
    )
    private val asked = mutableListOf<Set<String>>()
    private val findLinks = FindWordLinksUseCase { keys ->
        asked += keys
        index.filterKeys { it in keys }
    }

    private fun word(dictionary: Dictionary, text: String, id: Long = 1) =
        Word(dictionary, id, text, text.take(1), text, null)

    @Test
    fun `a word key is lowercase and has no stress mark`() {
        assertEquals("янтар", wordKey("Я́нтар"))
    }

    @Test
    fun `run parts are the single words between commas, with their offsets`() {
        assertEquals(
            listOf(0..4 to "янтар", 8..14 to "бурштын"),
            headwordRunParts("янтар,  бурштын, брацца за, ")
        )
    }

    @Test
    fun `links the known Belarusian words of a translation`() = runTest {
        val html = """<font color="#831b03">Янтар, бурштын</font> <span style="color: #831b03">лодар</span> """ +
            """<font color="#831b03">цельпука</font>"""

        val links = findLinks(word(Dictionary.RusBel, "янтарь"), html)

        assertEquals(index, links)
        assertEquals(listOf(setOf("янтар", "бурштын", "лодар", "цельпука")), asked)
    }

    @Test
    fun `Рус-Бел uses data-word, without the stress mark`() = runTest {
        val html = """<font color="#831b03" data-word="я́нтар">я́нтар</font> <font color="#831b03">бурштын</font>"""

        assertEquals(mapOf("янтар" to index.getValue("янтар")), findLinks(word(Dictionary.RusBel, "янтарь"), html))
    }

    @Test
    fun `the entry does not link to itself`() = runTest {
        val html = """<font color="#831b03">бурштын</font>, <font color="#831b03">янтар</font>"""

        val links = findLinks(word(Dictionary.BelRus, "бурштын", id = 27890), html)

        assertEquals(mapOf("янтар" to index.getValue("янтар")), links)
    }

    @Test
    fun `ТСБМ and entries without headword colour ask nothing`() = runTest {
        assertEquals(emptyMap(), findLinks(word(Dictionary.Tsbm, "янтар"), """<font color="#831b03">бурштын</font>"""))
        assertEquals(emptyMap(), findLinks(word(Dictionary.RusBel, "янтарь"), "<b>янтар</b>"))
        assertEquals(emptyList(), asked)
    }

    @Test
    fun `no links when the index cannot be read`() = runTest {
        val failing = FindWordLinksUseCase { throw IOException("no index") }

        assertEquals(emptyMap(), failing(word(Dictionary.RusBel, "янтарь"), """<font color="#831b03">янтар</font>"""))
    }
}
