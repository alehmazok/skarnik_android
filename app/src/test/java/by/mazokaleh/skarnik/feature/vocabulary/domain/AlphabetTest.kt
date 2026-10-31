package by.mazokaleh.skarnik.feature.vocabulary.domain

import by.mazokaleh.skarnik.core.model.Dictionary
import kotlin.test.assertEquals
import kotlin.test.assertNull
import org.junit.Test

class AlphabetTest {
    @Test
    fun `Belarusian letters sort in alphabet order, not by code point`() {
        val words = listOf("ўзор", "яма", "ёлка", "енот", "жаба", "іней", "зіма", "йод", "уток", "фара")

        assertEquals(
            listOf("енот", "ёлка", "жаба", "зіма", "іней", "йод", "уток", "ўзор", "фара", "яма"),
            words.sortedBy(Alphabet.Belarusian::sortKey)
        )
    }

    @Test
    fun `Russian ё comes between е and ж`() {
        val words = listOf("ёж", "жаба", "ель", "еж")

        assertEquals(listOf("еж", "ель", "ёж", "жаба"), words.sortedBy(Alphabet.Russian::sortKey))
    }

    @Test
    fun `letters inside a word follow the alphabet too`() {
        // By code point і (U+0456) comes after я (U+044F).
        assertEquals(listOf("бяда", "біць"), listOf("біць", "бяда").sorted())
        assertEquals(listOf("біць", "бяда"), listOf("бяда", "біць").sortedBy(Alphabet.Belarusian::sortKey))
    }

    @Test
    fun `a hyphen sorts before letters`() {
        val words = listOf("паабедаць", "па-беларуску")

        assertEquals(listOf("па-беларуску", "паабедаць"), words.sortedBy(Alphabet.Belarusian::sortKey))
    }

    @Test
    fun `rank is null outside the alphabet`() {
        assertEquals(0, Alphabet.Russian.rank('а'))
        assertNull(Alphabet.Russian.rank('ў'))
        assertNull(Alphabet.Belarusian.rank('щ'))
        assertNull(Alphabet.Belarusian.rank('a'))
    }

    @Test
    fun `each dictionary uses the alphabet of its headwords`() {
        assertEquals(Alphabet.Belarusian, Dictionary.BelRus.alphabet)
        assertEquals(Alphabet.Belarusian, Dictionary.Tsbm.alphabet)
        assertEquals(Alphabet.Russian, Dictionary.RusBel.alphabet)
    }
}
