package by.mazokaleh.skarnik.feature.vocabulary.data

import by.mazokaleh.skarnik.core.database.ObjectBoxStoreOpener
import by.mazokaleh.skarnik.core.database.ObjectBoxStores
import by.mazokaleh.skarnik.core.database.SkarnikDatabase
import by.mazokaleh.skarnik.core.model.Dictionary
import by.mazokaleh.skarnik.feature.vocabulary.domain.LoadVocabularyUseCase
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import org.junit.AfterClass
import org.junit.BeforeClass
import org.junit.ClassRule
import org.junit.Test
import org.junit.rules.TemporaryFolder

/** Loads the dictionaries from the real bundled index. */
class ObjectBoxVocabularyTest {
    private val repository = ObjectBoxVocabularyRepository(database, Dispatchers.Unconfined)
    private val load = LoadVocabularyUseCase(repository, Dispatchers.Unconfined)

    @Test
    fun `every dictionary loads whole, each word once`() = runTest {
        Dictionary.entries.forEach { dictionary ->
            val entries = repository.entries(dictionary)
            val vocabulary = load(dictionary)

            assertTrue(entries.size > MIN_WORDS, "${dictionary.path}: ${entries.size}")
            assertEquals(entries.size, vocabulary.sections.sumOf { it.words.size }, dictionary.path)
            val listedIds = vocabulary.sections.flatMap { section -> section.words.map { it.wordId } }
            assertEquals(entries.map { it.wordId }.toSet(), listedIds.toSet())
        }
    }

    @Test
    fun `the index has the letters of each language`() = runTest {
        val belarusian = load(Dictionary.BelRus).indexSections.map { it.letter }
        val russian = load(Dictionary.RusBel).indexSections.map { it.letter }

        assertEquals("А", belarusian.first())
        // No headword starts with «ў», so it has no index entry.
        assertTrue("І" in belarusian && "Щ" !in belarusian && "И" !in belarusian, belarusian.toString())
        assertTrue("Щ" in russian && "Ў" !in russian, russian.toString())
        assertTrue(belarusian.indexOf("З") < belarusian.indexOf("І"), belarusian.toString())
        assertTrue(belarusian.indexOf("Е") + 1 == belarusian.indexOf("Ё"), belarusian.toString())
    }

    @Test
    fun `a known word is in its letter section`() = runTest {
        val section = load(Dictionary.BelRus).sections.first { it.letter == "П" }

        assertTrue(section.words.any { it.word == "прывітанне" })
    }

    @Test
    fun `first rows point at each section header`() = runTest {
        val sections = load(Dictionary.Tsbm).sections

        sections.zipWithNext { current, next -> assertEquals(current.firstRow + 1 + current.words.size, next.firstRow) }
        assertEquals(0, sections.first().firstRow)
    }

    companion object {
        /** Each dictionary has about 100k words; a much smaller count means the query is wrong. */
        private const val MIN_WORDS = 50_000

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
