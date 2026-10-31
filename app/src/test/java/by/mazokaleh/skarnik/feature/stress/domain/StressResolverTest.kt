package by.mazokaleh.skarnik.feature.stress.domain

import java.io.IOException
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlinx.coroutines.test.runTest
import org.junit.Test

/** Every branch of stress_spec.md §4a (word resolution) and §4b (table fetch). */
class StressResolverTest {
    private val primary = FakeBackend(StressSource.Starnik)
    private val fallback = FakeBackend(StressSource.Admin)
    private val resolver = StressResolver(primary, fallback)

    @Test
    fun `primary exact matches win and the fallback is not asked`() = runTest {
        primary.words = listOf(primary.entry(1, "пара", "па́ра"), primary.entry(2, "пара", "пара́"))

        val result = resolver.resolveWords("пара")

        assertEquals(StressResult.Found(primary.words), result)
        assertEquals(0, fallback.wordListCalls)
    }

    @Test
    fun `only exact case-sensitive matches count`() = runTest {
        primary.words =
            listOf(
                primary.entry(1, "Пара", "Па́ра"),
                primary.entry(2, "пара", "пара́"),
                primary.entry(3, "парасон", "парасо́н")
            )

        val result = resolver.resolveWords("пара")

        assertEquals(StressResult.Found(listOf(primary.words[1])), result)
    }

    @Test
    fun `no exact match in the primary falls back`() = runTest {
        primary.words = listOf(primary.entry(1, "парасон", "парасо́н"))
        fallback.words = listOf(fallback.entry(7, "пара", "па́ра"))

        val result = resolver.resolveWords("пара")

        assertEquals(StressResult.Found(fallback.words), result)
    }

    @Test
    fun `primary failure falls back`() = runTest {
        primary.failure = IOException("starnik.by is down")
        fallback.words = listOf(fallback.entry(7, "пара", "па́ра"))

        assertEquals(StressResult.Found(fallback.words), resolver.resolveWords("пара"))
    }

    @Test
    fun `results are never merged`() = runTest {
        primary.words = listOf(primary.entry(1, "пара", "па́ра"))
        fallback.words = listOf(fallback.entry(7, "пара", "пара́"))

        assertEquals(StressResult.Found(primary.words), resolver.resolveWords("пара"))
    }

    @Test
    fun `nothing in either source is not found`() = runTest {
        fallback.words = listOf(fallback.entry(7, "парасон", "парасо́н"))

        assertEquals(StressResult.NotFound, resolver.resolveWords("пара"))
    }

    @Test
    fun `fallback failure is an error`() = runTest {
        primary.failure = IOException("down")
        fallback.failure = IOException("down too")

        assertIs<StressResult.Failed>(resolver.resolveWords("пара"))
    }

    @Test
    fun `fallback failure after an empty primary is an error too`() = runTest {
        fallback.failure = IOException("down")

        assertIs<StressResult.Failed>(resolver.resolveWords("пара"))
    }

    @Test
    fun `table comes from the entry's own source`() = runTest {
        primary.rows = listOf(StressRow("Форма", "па́ра"))
        fallback.rows = listOf(StressRow("Форма", "пара́"))

        assertEquals(StressResult.Found(primary.rows), resolver.table(1, StressSource.Starnik))
        assertEquals(StressResult.Found(fallback.rows), resolver.table(7, StressSource.Admin))
        assertEquals(listOf(1L), primary.tableIds)
        assertEquals(listOf(7L), fallback.tableIds)
    }

    @Test
    fun `an empty table is not found, with no retry on the other source`() = runTest {
        fallback.rows = listOf(StressRow("Форма", "пара́"))

        assertEquals(StressResult.NotFound, resolver.table(1, StressSource.Starnik))
        assertEquals(emptyList(), fallback.tableIds)
    }

    @Test
    fun `a failing table is an error, with no retry on the other source`() = runTest {
        primary.failure = IOException("down")
        fallback.rows = listOf(StressRow("Форма", "пара́"))

        assertIs<StressResult.Failed>(resolver.table(1, StressSource.Starnik))
        assertEquals(emptyList(), fallback.tableIds)
    }

    private class FakeBackend(private val source: StressSource) : StressBackend {
        var words: List<StressWordEntry> = emptyList()
        var rows: List<StressRow> = emptyList()
        var failure: Throwable? = null
        var wordListCalls = 0
        val tableIds = mutableListOf<Long>()

        fun entry(id: Long, lemma: String, word: String) = StressWordEntry(id, lemma, word, "Nouns", source)

        override suspend fun resolveWordList(word: String): List<StressWordEntry> {
            wordListCalls++
            failure?.let { throw it }
            return words
        }

        override suspend fun stressTable(id: Long): List<StressRow> {
            tableIds += id
            failure?.let { throw it }
            return rows
        }
    }
}
