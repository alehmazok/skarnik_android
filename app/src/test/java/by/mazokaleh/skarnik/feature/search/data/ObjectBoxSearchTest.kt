package by.mazokaleh.skarnik.feature.search.data

import by.mazokaleh.skarnik.core.database.ObjectBoxStoreOpener
import by.mazokaleh.skarnik.core.database.ObjectBoxStores
import by.mazokaleh.skarnik.core.database.SkarnikDatabase
import by.mazokaleh.skarnik.feature.search.domain.SearchWordsUseCase
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import org.junit.AfterClass
import org.junit.BeforeClass
import org.junit.ClassRule
import org.junit.Test
import org.junit.rules.TemporaryFolder

/** Search against the real bundled index, so the queries are checked on Flutter-written data. */
class ObjectBoxSearchTest {
    private val index = ObjectBoxSearchIndex(database, Dispatchers.Unconfined)
    private val search = SearchWordsUseCase(index, Dispatchers.Unconfined)

    @Test
    fun `prefix search returns words that start with the query, sorted`() = runTest {
        val words = index.findByWord(setOf("прывітан"), SearchWordsUseCase.RESULT_LIMIT)

        assertTrue(words.isNotEmpty())
        assertTrue(words.all { it.lword.startsWith("прывітан") }, words.toString())
        assertEquals(words.map { it.lword }.sorted(), words.map { it.lword })
    }

    @Test
    fun `a Belarusian word typed on a Russian keyboard is found`() = runTest {
        val result = search("прывитанне")

        assertTrue(result.words.any { it.word == "прывітанне" }, result.words.toString())
        assertFalse(result.usedPrepositionFallback)
    }

    @Test
    fun `results come from all three dictionaries`() = runTest {
        val result = search("дом")

        assertEquals(3, result.words.map { it.dictionary }.distinct().size, result.words.toString())
    }

    @Test
    fun `mask search leaves out what the word search already found`() = runTest {
        val limit = SearchWordsUseCase.RESULT_LIMIT
        val byWord = index.findByWord(setOf("сло"), limit)
        val byMask = index.findByWordMask(setOf("сло"), byWord.mapTo(HashSet()) { it.id }, limit)

        assertTrue(byMask.none { found -> byWord.any { it.id == found.id } })
    }

    @Test
    fun `a typo is corrected by fuzzy search`() = runTest {
        val result = search("прывітане")

        assertTrue(result.words.any { it.word == "прывітанне" }, result.words.toString())
    }

    @Test
    fun `a glued preposition is stripped when nothing else matches`() = runTest {
        val result = search("всмысле")

        assertTrue(result.usedPrepositionFallback)
        assertTrue(result.words.isNotEmpty())
        assertTrue(result.words.all { it.lword.startsWith("смысл") }, result.words.toString())
    }

    @Test
    fun `nonsense finds nothing`() = runTest {
        assertTrue(search("щщщщщщщщ").words.isEmpty())
    }

    companion object {
        @get:ClassRule
        @JvmStatic
        val tmp = TemporaryFolder()

        private lateinit var stores: ObjectBoxStores
        private val database = object : SkarnikDatabase {
            override suspend fun stores(): ObjectBoxStores = stores
        }

        // Unit tests run with the module directory as the working directory.
        private val SEED_ASSET = File("src/main/assets/${ObjectBoxStoreOpener.SEED_ASSET}")

        @BeforeClass
        @JvmStatic
        fun openSeed() {
            stores = ObjectBoxStoreOpener(tmp.newFolder("app_flutter")) { SEED_ASSET.inputStream() }.open()
        }

        @AfterClass
        @JvmStatic
        fun closeSeed() {
            stores.close()
        }
    }
}
