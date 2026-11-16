package by.mazokaleh.skarnik.feature.stress.domain

import by.mazokaleh.skarnik.core.model.Dictionary
import by.mazokaleh.skarnik.core.model.Word
import java.io.IOException
import kotlin.test.assertEquals
import kotlinx.coroutines.test.runTest
import org.junit.Test

/** The rules of skarnik_ios `SKSkarnikTranslation.belWords`. */
class StressCandidatesTest {
    private val asked = mutableListOf<Set<String>>()
    private var known: Set<String> = setOf("абдымкі", "рука", "рукаў", "ве́чар", "янтар", "бурштын", "лодар", "гультай")
    private val candidates = GetStressCandidatesUseCase { lwords ->
        asked += lwords
        lwords intersect known
    }

    private fun word(dictionary: Dictionary, text: String) = Word(dictionary, 1, text, text.take(1), text, null)

    @Test
    fun `a single-word ТСБМ headword is the candidate, whatever the translation holds`() = runTest {
        val html = """<font color="#831b03">рукаў</font>"""

        assertEquals(listOf("рука"), candidates(word(Dictionary.Tsbm, "рука"), html))
        assertEquals(emptyList(), asked)
    }

    @Test
    fun `a ТСБМ phrase has no candidates`() = runTest {
        assertEquals(emptyList(), candidates(word(Dictionary.Tsbm, "без канца"), ""))
    }

    @Test
    fun `Рус-Бел offers the known single words of headword-coloured runs, sorted and de-duplicated`() = runTest {
        val html = """<font size="+2" color="#831b03">рука, рукаў</font> <i>ж.</i> """ +
            """<font color="831B03">абдымкі</font>; <font color="#831b03">брацца&nbsp;за</font> """ +
            """<font color="#831b03">рука</font> <font color="#008000">разм.</font> <font>рука</font>"""

        assertEquals(listOf("абдымкі", "рука", "рукаў"), candidates(word(Dictionary.RusBel, "рука"), html))
        assertEquals(listOf(setOf("рука", "рукаў", "абдымкі")), asked)
    }

    @Test
    fun `Рус-Бел does not offer its Russian headword`() = runTest {
        known = known + "янтарь"

        val html = """<font color="#831b03">янтар</font>"""

        assertEquals(listOf("янтар"), candidates(word(Dictionary.RusBel, "янтарь"), html))
    }

    @Test
    fun `runs coloured by a TinyMCE style count too`() = runTest {
        val html = """<span style="color:#831b03">лодар</span>, <span style="color: #831B03;">гультай</span> """ +
            """<span style="color: #8b0000;">рука</span>"""

        assertEquals(listOf("гультай", "лодар"), candidates(word(Dictionary.RusBel, "лоботряс"), html))
    }

    @Test
    fun `Рус-Бел prefers data-word attributes over coloured runs`() = runTest {
        val html = """<font color="#831b03" data-word="янтар">я́нтар</font> <a data-word="бурштын">бурштын</a> """ +
            """<font color="#831b03">рука</font> <a data-word="брацца за">брацца за</a>"""

        assertEquals(listOf("бурштын", "янтар"), candidates(word(Dictionary.RusBel, "янтарь"), html))
    }

    @Test
    fun `Бел-Рус offers its headword and the known synonyms of its coloured runs`() = runTest {
        val html = """<font color="#831b03">бурштын</font>, <font color="#831b03">янтар</font>, """ +
            """<font color="#831b03">цельпука</font>"""

        assertEquals(listOf("бурштын", "янтар"), candidates(word(Dictionary.BelRus, "бурштын"), html))
        assertEquals(listOf(setOf("янтар", "цельпука")), asked)
    }

    @Test
    fun `tags inside a run are dropped and phrases are skipped`() = runTest {
        val html = """<font color="#831b03"><b>ве́чар</b>, добры дзень</font>"""

        assertEquals(listOf("ве́чар"), candidates(word(Dictionary.RusBel, "вечер"), html))
    }

    @Test
    fun `sorted ignoring case`() = runTest {
        known = setOf("брэст", "аршанскі")

        val html = """<font color="#831b03">Брэст, аршанскі</font>"""

        assertEquals(listOf("аршанскі", "Брэст"), candidates(word(Dictionary.RusBel, "брест"), html))
    }

    @Test
    fun `without the index only the headword is offered`() = runTest {
        val failing = GetStressCandidatesUseCase { throw IOException("no index") }

        val html = """<font color="#831b03">янтар</font>"""

        assertEquals(listOf("бурштын"), failing(word(Dictionary.BelRus, "бурштын"), html))
    }

    @Test
    fun `no headword colour, no candidates`() = runTest {
        assertEquals(emptyList(), candidates(word(Dictionary.RusBel, "янтарь"), "<b>янтар</b>"))
        assertEquals(emptyList(), asked)
    }
}
