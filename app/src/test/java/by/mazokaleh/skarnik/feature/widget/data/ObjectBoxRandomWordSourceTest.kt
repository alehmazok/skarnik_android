package by.mazokaleh.skarnik.feature.widget.data

import by.mazokaleh.skarnik.core.database.ObjectBoxStoreOpener
import by.mazokaleh.skarnik.core.database.ObjectBoxStores
import by.mazokaleh.skarnik.core.database.SkarnikDatabase
import by.mazokaleh.skarnik.core.model.Dictionary
import java.io.File
import kotlin.random.Random
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import org.junit.AfterClass
import org.junit.BeforeClass
import org.junit.ClassRule
import org.junit.Test
import org.junit.rules.TemporaryFolder

/** Random words from the real bundled index. */
class ObjectBoxRandomWordSourceTest {
    @Test
    fun `picks Bel-Rus words of the index`() = runTest {
        val source = ObjectBoxRandomWordSource(database, Dispatchers.Unconfined, Random(seed = 7))
        val words = List(20) { assertNotNull(source.randomBelRusWord()) }

        assertTrue(words.all { it.dictionary == Dictionary.BelRus })
        assertTrue(words.all { it.word.isNotBlank() && it.wordId > 0 })
        assertTrue(words.map { it.wordId }.toSet().size > 1, "always the same word")
    }

    @Test
    fun `the same random numbers pick the same word`() = runTest {
        val first = ObjectBoxRandomWordSource(database, Dispatchers.Unconfined, Random(seed = 1)).randomBelRusWord()
        val second = ObjectBoxRandomWordSource(database, Dispatchers.Unconfined, Random(seed = 1)).randomBelRusWord()

        assertEquals(first, second)
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
