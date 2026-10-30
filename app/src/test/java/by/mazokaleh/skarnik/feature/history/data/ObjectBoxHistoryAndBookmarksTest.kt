package by.mazokaleh.skarnik.feature.history.data

import by.mazokaleh.skarnik.core.database.ObjectBoxStores
import by.mazokaleh.skarnik.core.database.SkarnikDatabase
import by.mazokaleh.skarnik.core.database.entity.MyObjectBox
import by.mazokaleh.skarnik.core.database.entity.ObjectboxHistoryWord_
import by.mazokaleh.skarnik.core.model.Dictionary
import by.mazokaleh.skarnik.core.model.Word
import by.mazokaleh.skarnik.feature.favorites.data.ObjectBoxBookmarksRepository
import io.objectbox.query.QueryBuilder
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

/** History and bookmarks on real (empty) ObjectBox stores with the Flutter model. */
class ObjectBoxHistoryAndBookmarksTest {
    @get:Rule
    val tmp = TemporaryFolder()

    private lateinit var stores: ObjectBoxStores
    private val database = object : SkarnikDatabase {
        override suspend fun stores() = stores
    }
    private val history = ObjectBoxHistoryRepository(database, Dispatchers.Unconfined)
    private val bookmarks = ObjectBoxBookmarksRepository(database, Dispatchers.Unconfined)

    private val hello = Word(Dictionary.BelRus, 10, "прывітанне", "п", "прывітанне", "прывитанне")
    private val bye = Word(Dictionary.BelRus, 11, "бывай", "б", "бывай", null)
    private val helloInTsbm = hello.copy(dictionary = Dictionary.Tsbm)

    @Before
    fun setUp() {
        fun open(name: String) = MyObjectBox.builder().directory(tmp.newFolder(name)).build()
        stores = ObjectBoxStores(
            search = open("search"),
            history = open("history"),
            translation = open("translation")
        )
    }

    @After
    fun tearDown() {
        stores.close()
    }

    private fun historyNewestFirst() = stores.historyBox.query()
        .order(ObjectboxHistoryWord_.id, QueryBuilder.DESCENDING)
        .build()
        .use { query -> query.find().map { it.langId to it.wordId } }

    @Test
    fun `history keeps every field and puts a re-opened word on top once`() = runTest {
        history.save(hello)
        history.save(bye)
        history.save(hello)

        assertEquals(listOf(1L to 10L, 1L to 11L), historyNewestFirst())
        val saved = stores.historyBox.all.first { it.wordId == 10L }
        assertEquals("прывітанне", saved.word)
        assertEquals("п", saved.letter)
        assertEquals("прывітанне", saved.lword)
        assertEquals("прывитанне", saved.lwordMask)
    }

    @Test
    fun `the same id in another dictionary is another history entry`() = runTest {
        history.save(hello)
        history.save(helloInTsbm)

        assertEquals(listOf(2L to 10L, 1L to 10L), historyNewestFirst())
    }

    @Test
    fun `bookmarks are added once and removed per dictionary`() = runTest {
        bookmarks.add(hello)
        bookmarks.add(hello)
        bookmarks.add(helloInTsbm)

        assertTrue(bookmarks.isBookmarked(hello))
        assertEquals(2, stores.favoritesBox.count())

        bookmarks.remove(hello)

        assertFalse(bookmarks.isBookmarked(hello))
        assertTrue(bookmarks.isBookmarked(helloInTsbm))
        assertFalse(bookmarks.isBookmarked(bye))
    }
}
