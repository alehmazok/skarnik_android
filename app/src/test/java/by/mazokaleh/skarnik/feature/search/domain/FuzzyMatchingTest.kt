package by.mazokaleh.skarnik.feature.search.domain

import by.mazokaleh.skarnik.core.model.Dictionary
import kotlin.test.assertEquals
import org.junit.Test

/** Ported from skarnik-flutter's `damerau_levenshtein_test.dart` and `fuzzy_candidate_ranking_test.dart`. */
class FuzzyMatchingTest {
    @Test
    fun `distance to or from an empty string is the other length`() {
        assertEquals(0, damerauLevenshteinDistance("", ""))
        assertEquals(3, damerauLevenshteinDistance("", "abc"))
        assertEquals(3, damerauLevenshteinDistance("abc", ""))
    }

    @Test
    fun `identical strings have distance 0`() {
        assertEquals(0, damerauLevenshteinDistance("скарнік", "скарнік"))
    }

    @Test
    fun `single substitution, insertion and deletion each cost 1`() {
        assertEquals(1, damerauLevenshteinDistance("кот", "кат"))
        assertEquals(1, damerauLevenshteinDistance("кот", "корт"))
        assertEquals(1, damerauLevenshteinDistance("корт", "кот"))
    }

    @Test
    fun `adjacent transposition costs 1`() {
        assertEquals(1, damerauLevenshteinDistance("teh", "the"))
        assertEquals(1, damerauLevenshteinDistance("слоўнік", "слоунік"))
    }

    @Test
    fun `realistic Belarusian typos`() {
        assertEquals(1, damerauLevenshteinDistance("скарнік", "скарнк"))
        assertEquals(1, damerauLevenshteinDistance("слоўнік", "слоўнык"))
    }

    @Test
    fun `is case-sensitive`() {
        assertEquals(1, damerauLevenshteinDistance("Кот", "кот"))
    }

    @Test
    fun `handles very different lengths and multiple edits`() {
        assertEquals(7, damerauLevenshteinDistance("ы", "слоўнік"))
        assertEquals(7, damerauLevenshteinDistance("слоўнік", "ы"))
        assertEquals(3, damerauLevenshteinDistance("kitten", "sitting"))
    }

    @Test
    fun `ranking drops candidates beyond the threshold`() {
        val close = word(1, "tost")
        val far = word(2, "zzzz")

        assertEquals(listOf(close), rankFuzzyCandidates(listOf(close, far), "test", maxDistance = 1, limit = 15))
    }

    @Test
    fun `ranking sorts by distance, then alphabetically`() {
        val exact = word(1, "test")
        val oneAway = word(2, "tost")
        val alsoOneAway = word(3, "aest")

        assertEquals(
            listOf(exact, alsoOneAway, oneAway),
            rankFuzzyCandidates(listOf(oneAway, exact, alsoOneAway), "test", maxDistance = 1, limit = 15)
        )
    }

    @Test
    fun `ranking keeps words whose length differs by up to the threshold`() {
        val longer = word(1, "tests")
        val tooLong = word(2, "testss")

        assertEquals(listOf(longer), rankFuzzyCandidates(listOf(longer, tooLong), "test", maxDistance = 1, limit = 15))
    }

    @Test
    fun `ranking caps the results`() {
        val candidates = (1L..20L).map { word(it, "test") }

        assertEquals(15, rankFuzzyCandidates(candidates, "test", maxDistance = 1, limit = 15).size)
    }

    private fun word(id: Long, lword: String) = SearchWord(
        id = id,
        dictionary = Dictionary.BelRus,
        wordId = id,
        word = lword,
        letter = lword.take(1),
        lword = lword
    )
}
