package by.mazokaleh.skarnik.feature.widget.domain

import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import org.junit.Test

class WordSimilarityTest {
    @Test
    fun `block tags become line breaks and blank lines go`() {
        val html = "<p><b>янтарь</b>&nbsp;</p><div>1) смола</div>2) камень<br>  <li>колер</li>"

        assertEquals("янтарь\n1) смола\n2) камень\nколер", WordSimilarity.htmlToPlainText(html))
    }

    @Test
    fun `inline tags keep the text on one line`() {
        assertEquals("гибель, конец", WordSimilarity.htmlToPlainText("<font color=\"#831b03\">гибель</font>, конец"))
    }

    @Test
    fun `normalize folds Belarusian and Russian spellings together`() {
        assertEquals("малака", WordSimilarity.normalize(" Малако "))
        assertEquals("малака", WordSimilarity.normalize("молоко"))
        assertEquals("кніга", WordSimilarity.normalize("книга"))
        assertEquals("шчука", WordSimilarity.normalize("щука"))
        assertEquals("аб'ем", WordSimilarity.normalize("аб’ём"))
        assertEquals("цам'я", WordSimilarity.normalize("сям'я"))
    }

    @Test
    fun `levenshtein counts edits`() {
        assertEquals(0, WordSimilarity.levenshtein("вада", "вада"))
        assertEquals(3, WordSimilarity.levenshtein("", "дом"))
        assertEquals(1, WordSimilarity.levenshtein("дом", "дым"))
        assertEquals(3, WordSimilarity.levenshtein("kitten", "sitting"))
    }

    @Test
    fun `the same word in both languages is similar`() {
        assertTrue(WordSimilarity.isSimilar("вада", "<p>вода</p>"))
        assertTrue(WordSimilarity.isSimilar("малако", "молоко"))
        // 7 letters, distance 2: 3.5 > 1.8.
        assertTrue(WordSimilarity.isSimilar("гаворка", "говор"))
    }

    @Test
    fun `a different word is not similar`() {
        assertFalse(WordSimilarity.isSimilar("бурштын", "<b>янтарь</b>"))
        // 5 letters, distance 3: 1.67 is under the 1.8 threshold.
        assertFalse(WordSimilarity.isSimilar("сям'я", "семья"))
        assertFalse(WordSimilarity.isSimilar("вада", ""))
    }

    @Test
    fun `only the first sense of each line counts`() {
        assertFalse(WordSimilarity.isSimilar("дом", "здание — дом"))
        assertTrue(WordSimilarity.isSimilar("дом", "<p>здание</p><p>дом — жилище</p>"))
    }
}
