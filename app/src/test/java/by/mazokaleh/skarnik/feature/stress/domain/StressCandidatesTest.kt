package by.mazokaleh.skarnik.feature.stress.domain

import by.mazokaleh.skarnik.core.model.Dictionary
import by.mazokaleh.skarnik.core.model.Word
import kotlin.test.assertEquals
import org.junit.Test

class StressCandidatesTest {
    private fun word(dictionary: Dictionary, text: String) = Word(dictionary, 1, text, text.take(1), text, null)

    @Test
    fun `a single-word ТСБМ headword is the candidate, whatever the translation holds`() {
        val html = """<font color="#831b03">іншае</font>"""

        assertEquals(listOf("рука"), stressCandidates(word(Dictionary.Tsbm, "рука"), html))
    }

    @Test
    fun `a ТСБМ phrase has no candidates`() {
        assertEquals(emptyList(), stressCandidates(word(Dictionary.Tsbm, "без канца"), ""))
    }

    @Test
    fun `translations offer the single words of headword-coloured runs, sorted and de-duplicated`() {
        val html = """<font size="+2" color="#831b03">рука, рукаў</font> <i>ж.</i> """ +
            """<font color="831B03">абдымкі</font>; <font color="#831b03">брацца&nbsp;за</font> """ +
            """<font color="#831b03">рука</font> <font color="#008000">разм.</font> <font>без колеру</font>"""

        assertEquals(listOf("абдымкі", "рука", "рукаў"), stressCandidates(word(Dictionary.RusBel, "рука"), html))
    }

    @Test
    fun `tags inside a run are dropped and phrases are skipped`() {
        val html = """<font color="#831b03"><b>ве́чар</b>, добры дзень</font>"""

        assertEquals(listOf("ве́чар"), stressCandidates(word(Dictionary.BelRus, "вечар"), html))
    }

    @Test
    fun `no headword colour, no candidates`() {
        assertEquals(emptyList(), stressCandidates(word(Dictionary.BelRus, "бурштын"), "<b>янтарь</b>"))
    }
}
