package by.mazokaleh.skarnik.feature.stress.ui

import androidx.compose.ui.graphics.Color
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import org.junit.Test

class StressHighlightTest {
    private fun colored(word: String): List<String> = highlightStress(word, Color.Red).spanStyles
        .filter { it.item.color == Color.Red }
        .map { word.substring(it.start, it.end) }

    @Test
    fun `only the stressed vowel is coloured`() {
        assertEquals(listOf("а́"), colored("пара́"))
        assertEquals("пара́", highlightStress("пара́", Color.Red).text)
    }

    @Test
    fun `a stressed vowel inside the word`() {
        assertEquals(listOf("а́"), colored("па́ра"))
    }

    @Test
    fun `every stressed vowel is coloured`() {
        assertEquals(listOf("а́", "о́"), colored("ва́дазо́р"))
    }

    @Test
    fun `a word without a stress mark stays plain`() {
        assertTrue(highlightStress("пара", Color.Red).spanStyles.isEmpty())
        assertTrue(highlightStress("мёд", Color.Red).spanStyles.isEmpty())
    }

    @Test
    fun `a leading mark colours nothing`() {
        assertTrue(highlightStress("́а", Color.Red).spanStyles.isEmpty())
    }
}
