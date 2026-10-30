package by.mazokaleh.skarnik.feature.history.data

import by.mazokaleh.skarnik.core.database.ObjectBoxStores
import by.mazokaleh.skarnik.core.database.SkarnikDatabase
import by.mazokaleh.skarnik.core.database.entity.MyObjectBox
import by.mazokaleh.skarnik.core.database.entity.ObjectboxHistoryWord_
import app.cash.turbine.test
import by.mazokaleh.skarnik.core.model.Dictionary
import by.mazokaleh.skarnik.core.model.SavedWordsPage
import by.mazokaleh.skarnik.core.model.Word
import by.mazokaleh.skarnik.feature.favorites.data.ObjectBoxBookmarksRepository
import by.mazokaleh.skarnik.feature.favorites.domain.BookmarksSortOrder
import io.objectbox.query.QueryBuilder
import kotlinx.coroutines.flow.first
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
    private val apple = Word(Dictionary.RusBel, 12, "яблоко", "я", "яблоко", null)

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

    private fun SavedWordsPage.wordIds() = words.map { it.wordId }

    @Test
    fun `history pages newest first and tells whether more follow`() = runTest {
        history.save(hello)
        history.save(bye)
        history.save(apple)

        val firstTwo = history.observe(limit = 2).first()
        assertEquals(listOf(12L, 11L), firstTwo.wordIds())
        assertTrue(firstTwo.hasMore)

        val all = history.observe(limit = 3).first()
        assertEquals(listOf(12L, 11L, 10L), all.wordIds())
        assertFalse(all.hasMore)
        assertEquals(Dictionary.RusBel, all.words.first().dictionary)
        assertEquals("яблоко", all.words.first().word)
    }

    @Test
    fun `history updates when a word is opened elsewhere`() = runTest {
        history.observe(limit = 25).test {
            assertEquals(emptyList(), awaitItem().words)
            history.save(hello)
            assertEquals(listOf(10L), awaitItem().wordIds())
            history.save(bye)
            assertEquals(listOf(11L, 10L), awaitItem().wordIds())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `bookmarks sort by date added or by word`() = runTest {
        bookmarks.add(hello)
        bookmarks.add(apple)
        bookmarks.add(bye)

        assertEquals(listOf(11L, 12L, 10L), bookmarks.observe(BookmarksSortOrder.DateAdded, 25).first().wordIds())
        // бывай, прывітанне, яблоко
        assertEquals(listOf(11L, 10L, 12L), bookmarks.observe(BookmarksSortOrder.Alphabetical, 25).first().wordIds())
    }

    @Test
    fun `bookmarks update when one is added or removed elsewhere`() = runTest {
        bookmarks.observe(BookmarksSortOrder.DateAdded, 25).test {
            assertEquals(emptyList(), awaitItem().words)
            bookmarks.add(hello)
            assertEquals(listOf(10L), awaitItem().wordIds())
            bookmarks.remove(hello)
            assertEquals(emptyList(), awaitItem().words)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `a restored bookmark returns to its place in the date order`() = runTest {
        bookmarks.add(hello)
        bookmarks.add(bye)
        bookmarks.add(apple)
        val removed = bookmarks.observe(BookmarksSortOrder.DateAdded, 25).first().words[1]
        assertEquals(11L, removed.wordId)

        bookmarks.remove(removed)
        assertEquals(listOf(12L, 10L), bookmarks.observe(BookmarksSortOrder.DateAdded, 25).first().wordIds())

        bookmarks.restore(removed)
        val restored = bookmarks.observe(BookmarksSortOrder.DateAdded, 25).first()
        assertEquals(listOf(12L, 11L, 10L), restored.wordIds())
        assertEquals(removed, restored.words[1])
    }

    @Test
    fun `restoring a bookmark that was added again keeps one entry`() = runTest {
        bookmarks.add(hello)
        val removed = bookmarks.observe(BookmarksSortOrder.DateAdded, 25).first().words.single()
        bookmarks.remove(removed)
        bookmarks.add(hello)

        bookmarks.restore(removed)

        assertEquals(listOf(removed), bookmarks.observe(BookmarksSortOrder.DateAdded, 25).first().words)
    }
}
