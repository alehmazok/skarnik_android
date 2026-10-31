package by.mazokaleh.skarnik.feature.vocabulary.domain

import kotlin.test.assertEquals
import org.junit.Test

class BuildVocabularyTest {
    private fun entry(wordId: Long, word: String, letter: String = word.take(1).lowercase()) =
        VocabularyEntry(wordId, word, letter, word.lowercase())

    @Test
    fun `sections follow the alphabet and words are sorted inside them`() {
        val vocabulary = buildVocabulary(
            Alphabet.Belarusian,
            listOf(entry(1, "ўзор"), entry(2, "аблокі"), entry(3, "іней"), entry(4, "абавязак"), entry(5, "Ўладзімір"))
        )

        assertEquals(listOf("А", "І", "Ў"), vocabulary.sections.map { it.letter })
        assertEquals(listOf("абавязак", "аблокі"), vocabulary.sections[0].words.map { it.word })
        assertEquals(listOf("ўзор", "Ўладзімір"), vocabulary.sections[2].words.map { it.word })
    }

    @Test
    fun `first rows count one header per section`() {
        val vocabulary = buildVocabulary(
            Alphabet.Russian,
            listOf(entry(1, "арбуз"), entry(2, "аист"), entry(3, "бак"), entry(4, "ёж"))
        )

        // [А] аист арбуз [Б] бак [Ё] ёж
        assertEquals(listOf(0, 3, 5), vocabulary.sections.map { it.firstRow })
    }

    @Test
    fun `equal words keep id order`() {
        val vocabulary = buildVocabulary(Alphabet.Belarusian, listOf(entry(9, "коса"), entry(3, "коса")))

        assertEquals(listOf(3L, 9L), vocabulary.sections.single().words.map { it.wordId })
    }

    @Test
    fun `letters outside the alphabet go last and stay out of the index`() {
        val vocabulary = buildVocabulary(
            Alphabet.Belarusian,
            listOf(entry(1, "ABC"), entry(2, "ява"), entry(3, "щука"), entry(4, "алей"))
        )

        assertEquals(listOf("А", "Я", "A", "Щ"), vocabulary.sections.map { it.letter })
        assertEquals(listOf("А", "Я"), vocabulary.indexSections.map { it.letter })
    }

    @Test
    fun `the stored letter decides the section`() {
        val vocabulary = buildVocabulary(Alphabet.Belarusian, listOf(entry(1, "-ава", letter = "а"), entry(2, "аса")))

        assertEquals(listOf("-ава", "аса"), vocabulary.sections.single().words.map { it.word })
    }
}
