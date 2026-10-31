package by.mazokaleh.skarnik.feature.offline.data

import app.cash.turbine.test
import by.mazokaleh.skarnik.core.database.ObjectBoxStoreOpener
import by.mazokaleh.skarnik.core.database.ObjectBoxStores
import by.mazokaleh.skarnik.core.database.SkarnikDatabase
import by.mazokaleh.skarnik.core.model.Dictionary
import by.mazokaleh.skarnik.feature.offline.domain.DownloadedWord
import by.mazokaleh.skarnik.feature.translation.data.ObjectBoxLocalTranslationSource
import by.mazokaleh.skarnik.feature.translation.domain.TranslationEntry
import java.io.File
import kotlin.test.assertEquals
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.AfterClass
import org.junit.BeforeClass
import org.junit.ClassRule
import org.junit.Test
import org.junit.rules.TemporaryFolder

/** Downloaded dictionaries in the real translation store, read back by the translation screen's source. */
class ObjectBoxOfflineDictionaryStoreTest {
    private val store = ObjectBoxOfflineDictionaryStore(database, Dispatchers.Unconfined)
    private val local = ObjectBoxLocalTranslationSource(database, Dispatchers.Unconfined)

    @After
    fun tearDown() {
        stores.translationBox.removeAll()
    }

    @Test
    fun `downloaded words are found by the translation source`() = runTest {
        store.putAll(Dictionary.BelRus, listOf(DownloadedWord(1, 27890, "буршты́н", "<b>янтарь</b>", null)))

        assertEquals(TranslationEntry("буршты́н", "<b>янтарь</b>", null), local.find(Dictionary.BelRus, 27890))
    }

    @Test
    fun `rewriting a page replaces the words instead of adding them`() = runTest {
        val page = (1L..3L).map { DownloadedWord(it, it, null, "v1", null) }
        store.putAll(Dictionary.Tsbm, page)
        store.putAll(Dictionary.Tsbm, page.map { it.copy(translation = "v2") })

        assertEquals(3, store.count(Dictionary.Tsbm))
        assertEquals("v2", local.find(Dictionary.Tsbm, 2)?.html)
    }

    @Test
    fun `counts and clears one dictionary only`() = runTest {
        store.putAll(Dictionary.Tsbm, listOf(DownloadedWord(1, 7, null, "т", null)))
        store.putAll(Dictionary.BelRus, (1L..2L).map { DownloadedWord(it, 7 + it, null, "б", null) })

        store.clear(Dictionary.BelRus)

        assertEquals(0, store.count(Dictionary.BelRus))
        assertEquals(1, store.count(Dictionary.Tsbm))
    }

    @Test
    fun `the count follows writes and deletes`() = runTest {
        store.observeCount(Dictionary.RusBel).test {
            assertEquals(0, awaitItem())
            store.putAll(Dictionary.RusBel, listOf(DownloadedWord(1, 1, null, "р", null)))
            assertEquals(1, awaitItem())
            store.clear(Dictionary.RusBel)
            assertEquals(0, awaitItem())
        }
    }

    companion object {
        @get:ClassRule
        @JvmStatic
        val tmp = TemporaryFolder()

        private lateinit var stores: ObjectBoxStores
        private val database = object : SkarnikDatabase {
            override suspend fun stores(): ObjectBoxStores = stores
        }

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
