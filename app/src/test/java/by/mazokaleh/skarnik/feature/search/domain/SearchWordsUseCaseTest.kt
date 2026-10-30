package by.mazokaleh.skarnik.feature.search.domain

import by.mazokaleh.skarnik.feature.search.domain.FakeSearchIndex.Companion.row
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import org.junit.Test

/** Behaviour ported from skarnik-flutter's `search_repository_test.dart`. */
class SearchWordsUseCaseTest {
    private fun search(vararg rows: FakeSearchIndex.Row, index: FakeSearchIndex = FakeSearchIndex(rows.toList())) =
        index to SearchWordsUseCase(index, Dispatchers.Unconfined)

    @Test
    fun `finds by word prefix, then by mask, without duplicates and in that order`() = runTest {
        val byWord = row("прывітанне", mask = "прывітанне")
        val byMask = row("вітаць", mask = "прывітаць")
        val (_, useCase) = search(byWord, byMask, row("бывай"))

        val result = useCase("прыв")

        assertEquals(listOf(byWord.word, byMask.word), result.words)
        assertFalse(result.usedPrepositionFallback)
    }

    @Test
    fun `lowercases the query and also searches with Belarusian letters`() = runTest {
        val (index, useCase) = search(row("ісці"), row("ёлка"))

        assertEquals(listOf("ісці"), useCase("Исци").words.map { it.lword })
        assertEquals(setOf("исци", "ісці"), index.prefixQueries.first())
        assertEquals(listOf("ёлка"), useCase("ЕЛКА").words.map { it.lword })
    }

    @Test
    fun `substitutes all Russian letters and apostrophes`() {
        assertEquals("і ё ў ‘ ‘", SearchWordsUseCase.applySubstitutions("и е щ ъ '"))
        assertEquals("skarnik", SearchWordsUseCase.applySubstitutions("skarnik"))
        assertEquals("", SearchWordsUseCase.applySubstitutions(""))
    }

    @Test
    fun `caps the word stage at the result limit`() = runTest {
        val rows = (1..20).map { row("слова$it") }
        val (_, useCase) = search(*rows.toTypedArray())

        assertEquals(SearchWordsUseCase.RESULT_LIMIT, useCase("слова").words.size)
    }

    @Test
    fun `falls back to fuzzy search on the substituted first letter`() = runTest {
        val target = row("ісці")
        val (index, useCase) = search(target, row("іспыт"))

        val result = useCase("исти")

        assertEquals(listOf(target.word), result.words)
        assertEquals(listOf("і"), index.fuzzyLetters.take(1))
    }

    @Test
    fun `allows two edits for queries longer than five letters`() = runTest {
        val target = row("слоўнік")
        val (_, useCase) = search(target)

        assertEquals(listOf(target.word), useCase("слоуник").words)
    }

    @Test
    fun `does not run fuzzy search for short queries or when something matched`() = runTest {
        val (index, useCase) = search(row("кот"), row("кіт"))

        useCase("кы")
        useCase("ко")

        assertTrue(index.fuzzyLetters.isEmpty())
    }

    @Test
    fun `retries without a glued preposition when nothing matched`() = runTest {
        val target = row("смысл")
        val (_, useCase) = search(target)

        val result = useCase("всмысле")

        assertEquals(listOf(target.word), result.words)
        assertTrue(result.usedPrepositionFallback)
    }

    @Test
    fun `retry strips only one letter and may itself find nothing`() = runTest {
        val (index, useCase) = search(row("ўзор"))

        val result = useCase("ууу")

        assertTrue(result.words.isEmpty())
        assertTrue(result.usedPrepositionFallback)
        assertEquals(listOf(setOf("ууу"), setOf("уу")), index.prefixQueries)
    }

    @Test
    fun `does not retry when the query starts with another letter or is one letter long`() = runTest {
        val (_, useCase) = search(row("кот"))

        assertFalse(useCase("бзз").usedPrepositionFallback)
        assertFalse(useCase("в").usedPrepositionFallback)
        assertEquals(null, SearchWordsUseCase.stripGluedPrefixLetter("в"))
        assertEquals("сціш", SearchWordsUseCase.stripGluedPrefixLetter("усціш"))
        assertEquals("ранак", SearchWordsUseCase.stripGluedPrefixLetter("іранак"))
    }
}
