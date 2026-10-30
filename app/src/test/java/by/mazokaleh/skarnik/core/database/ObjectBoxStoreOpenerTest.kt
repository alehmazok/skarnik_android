package by.mazokaleh.skarnik.core.database

import by.mazokaleh.skarnik.core.database.entity.ObjectboxFavoriteWord
import by.mazokaleh.skarnik.core.database.entity.ObjectboxHistoryWord
import by.mazokaleh.skarnik.core.database.entity.ObjectboxSearchWord_
import by.mazokaleh.skarnik.core.database.entity.ObjectboxTranslationWord
import io.objectbox.query.QueryBuilder.StringOrder
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Opens the real search index that skarnik-flutter ships (written by ObjectBox Dart) with the Kotlin model.
 * If the Kotlin entities drift from the Flutter model UIDs, this fails instead of a user's upgrade.
 */
class ObjectBoxStoreOpenerTest {
    @get:Rule
    val tmp = TemporaryFolder()

    private lateinit var documentsDir: File
    private var stores: ObjectBoxStores? = null

    @Before
    fun setUp() {
        documentsDir = tmp.newFolder("app_flutter")
    }

    @After
    fun tearDown() {
        stores?.close()
    }

    private fun open(): ObjectBoxStores = ObjectBoxStoreOpener(documentsDir) { SEED_ASSET.inputStream() }
        .open()
        .also { stores = it }

    @Test
    fun `installs the seed and opens it at the Flutter paths`() {
        val opened = open()

        assertTrue(File(documentsDir, "objectbox_search/data.mdb").isFile)
        assertTrue(File(documentsDir, "objectbox_history/data.mdb").isFile)
        assertTrue(File(documentsDir, "objectbox_translation/data.mdb").isFile)
        assertFalse(File(documentsDir, "objectbox_search/data.mdb.tmp").exists())
        assertTrue(opened.searchBox.count() > MIN_SEED_WORDS, "seed has ${opened.searchBox.count()} words")
    }

    @Test
    fun `reads Flutter-written search words through the indexed properties`() {
        val opened = open()

        val words = opened.searchBox.query(
            ObjectboxSearchWord_.lword.startsWith("прывітанн", StringOrder.CASE_SENSITIVE),
        ).build().use { it.find() }

        assertTrue(words.isNotEmpty())
        assertTrue(words.all { it.word.isNotBlank() && it.langId in 0L..2L && it.wordId > 0 })
    }

    @Test
    fun `does not overwrite an existing search store`() {
        open().close()
        stores = null
        val dataFile = File(documentsDir, "objectbox_search/data.mdb")
        val modified = dataFile.lastModified()

        ObjectBoxStoreOpener(documentsDir) { error("seed must not be read again") }.open().also { stores = it }

        assertEquals(modified, dataFile.lastModified())
    }

    @Test
    fun `history, bookmarks and translations survive reopening`() {
        open().apply {
            historyBox.put(ObjectboxHistoryWord(langId = 1, letter = "п", wordId = 7, word = "прывітанне"))
            favoritesBox.put(ObjectboxFavoriteWord(langId = 1, letter = "п", wordId = 7, word = "прывітанне"))
            translationBox.put(translation(text = "first"))
            // Same key replaces in place, as in Flutter.
            translationBox.put(translation(text = "second"))
            close()
        }
        stores = null

        val reopened = open()

        assertEquals("прывітанне", reopened.historyBox.all.single().word)
        assertEquals(7L, reopened.favoritesBox.all.single().wordId)
        assertEquals("second", reopened.translationBox.all.single().translation)
    }

    private fun translation(text: String) = ObjectboxTranslationWord(
        langId = 1,
        wordId = 7,
        key = ObjectboxTranslationWord.keyOf(1, 7),
        translation = text,
    )

    private companion object {
        // Unit tests run with the module directory as the working directory.
        val SEED_ASSET = File("src/main/assets/${ObjectBoxStoreOpener.SEED_ASSET}")
        const val MIN_SEED_WORDS = 100_000L
    }
}
