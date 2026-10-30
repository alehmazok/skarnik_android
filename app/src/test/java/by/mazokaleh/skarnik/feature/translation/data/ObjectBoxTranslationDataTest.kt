package by.mazokaleh.skarnik.feature.translation.data

import by.mazokaleh.skarnik.core.database.ObjectBoxStoreOpener
import by.mazokaleh.skarnik.core.database.ObjectBoxStores
import by.mazokaleh.skarnik.core.database.SkarnikDatabase
import by.mazokaleh.skarnik.core.database.entity.ObjectboxSearchWord_
import by.mazokaleh.skarnik.core.database.entity.ObjectboxTranslationWord
import by.mazokaleh.skarnik.core.model.Dictionary
import by.mazokaleh.skarnik.feature.translation.domain.TranslationEntry
import io.objectbox.query.QueryBuilder.StringOrder
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import org.junit.AfterClass
import org.junit.BeforeClass
import org.junit.ClassRule
import org.junit.Test
import org.junit.rules.TemporaryFolder

/** Word lookup on the real bundled index, and the offline-dictionary store with Flutter's key format. */
class ObjectBoxTranslationDataTest {
    private val words = ObjectBoxWordRepository(database, Dispatchers.Unconfined)
    private val local = ObjectBoxLocalTranslationSource(database, Dispatchers.Unconfined)

    @Test
    fun `finds a word of the index by dictionary and id`() = runTest {
        val row = stores.searchBox
            .query(ObjectboxSearchWord_.word.equal("прывітанне", StringOrder.CASE_SENSITIVE))
            .build()
            .use { it.findFirst() }
        assertNotNull(row)
        val dictionary = assertNotNull(Dictionary.byLangId(row.langId))

        val word = assertNotNull(words.find(dictionary, row.wordId))

        assertEquals("прывітанне", word.word)
        assertEquals(row.letter, word.letter)
        assertEquals(row.lword, word.lword)
    }

    @Test
    fun `a word from another dictionary or a missing id is not found`() = runTest {
        assertNull(words.find(Dictionary.BelRus, Long.MAX_VALUE))
    }

    @Test
    fun `reads a downloaded entry by langId and wordId`() = runTest {
        stores.translationBox.put(
            ObjectboxTranslationWord(
                langId = Dictionary.Tsbm.langId,
                wordId = 5,
                key = ObjectboxTranslationWord.keyOf(Dictionary.Tsbm.langId, 5),
                stress = "сло́ва",
                translation = "<b>тлумачэнне</b>"
            )
        )

        val expected = TranslationEntry("сло́ва", "<b>тлумачэнне</b>", null)
        assertEquals(expected, local.find(Dictionary.Tsbm, 5))
        assertNull(local.find(Dictionary.BelRus, 5))
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
