package by.mazokaleh.skarnik.feature.favorites.data

import by.mazokaleh.skarnik.core.common.IoDispatcher
import by.mazokaleh.skarnik.core.database.SkarnikDatabase
import by.mazokaleh.skarnik.core.database.changes
import by.mazokaleh.skarnik.core.database.entity.ObjectboxFavoriteWord
import by.mazokaleh.skarnik.core.database.entity.ObjectboxFavoriteWord_
import by.mazokaleh.skarnik.core.model.Dictionary
import by.mazokaleh.skarnik.core.model.SavedWord
import by.mazokaleh.skarnik.core.model.SavedWordsPage
import by.mazokaleh.skarnik.core.model.Word
import by.mazokaleh.skarnik.feature.favorites.domain.BookmarksRepository
import by.mazokaleh.skarnik.feature.favorites.domain.BookmarksSortOrder
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.objectbox.Box
import io.objectbox.query.Query
import io.objectbox.query.QueryBuilder
import javax.inject.Inject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

/** Flutter's `ObjectboxFavoritesRepository`, on the history store. */
class ObjectBoxBookmarksRepository @Inject constructor(
    private val database: SkarnikDatabase,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : BookmarksRepository {
    override suspend fun isBookmarked(word: Word): Boolean = withContext(ioDispatcher) {
        database.stores().favoritesBox.queryFor(word).use { it.count() > 0 }
    }

    override suspend fun add(word: Word) = withContext(ioDispatcher) {
        val stores = database.stores()
        val box = stores.favoritesBox
        stores.history.runInTx {
            box.queryFor(word).use { it.remove() }
            box.put(
                ObjectboxFavoriteWord(
                    langId = word.dictionary.langId,
                    letter = word.letter,
                    wordId = word.wordId,
                    word = word.word
                )
            )
        }
    }

    override suspend fun remove(word: Word) = withContext(ioDispatcher) {
        database.stores().favoritesBox.queryFor(word).use { it.remove() }
        Unit
    }

    override fun observe(sortOrder: BookmarksSortOrder, limit: Int): Flow<SavedWordsPage> = flow {
        val stores = database.stores()
        emitAll(
            stores.history.changes<ObjectboxFavoriteWord>().map {
                // A query is not shared between threads, so each read builds its own.
                val rows = stores.favoritesBox.query().apply {
                    when (sortOrder) {
                        BookmarksSortOrder.DateAdded -> order(ObjectboxFavoriteWord_.id, QueryBuilder.DESCENDING)
                        BookmarksSortOrder.Alphabetical -> order(ObjectboxFavoriteWord_.word)
                    }
                }.build().use { it.find(0, limit + 1L) }
                // The extra row tells whether there is a next page.
                SavedWordsPage(rows.take(limit).mapNotNull { it.toSavedWord() }, hasMore = rows.size > limit)
            }
        )
    }.flowOn(ioDispatcher)

    override suspend fun remove(bookmark: SavedWord) = withContext(ioDispatcher) {
        database.stores().favoritesBox.remove(bookmark.id)
        Unit
    }

    override suspend fun restore(bookmark: SavedWord) = withContext(ioDispatcher) {
        val stores = database.stores()
        val box = stores.favoritesBox
        stores.history.runInTx {
            // The word may have been bookmarked again meanwhile; keep one entry, in the old place.
            box.query(
                ObjectboxFavoriteWord_.wordId.equal(bookmark.wordId)
                    .and(ObjectboxFavoriteWord_.langId.equal(bookmark.dictionary.langId))
            ).build().use { it.remove() }
            box.put(
                ObjectboxFavoriteWord(
                    id = bookmark.id,
                    langId = bookmark.dictionary.langId,
                    letter = bookmark.letter,
                    wordId = bookmark.wordId,
                    word = bookmark.word
                )
            )
        }
        Unit
    }

    private fun ObjectboxFavoriteWord.toSavedWord(): SavedWord? = Dictionary.byLangId(langId)?.let {
        SavedWord(id = id, dictionary = it, wordId = wordId, word = word, letter = letter)
    }

    private fun Box<ObjectboxFavoriteWord>.queryFor(word: Word): Query<ObjectboxFavoriteWord> = query(
        ObjectboxFavoriteWord_.wordId.equal(word.wordId)
            .and(ObjectboxFavoriteWord_.langId.equal(word.dictionary.langId))
    ).build()
}

@Module
@InstallIn(SingletonComponent::class)
interface BookmarksModule {
    @Binds
    fun bindBookmarksRepository(impl: ObjectBoxBookmarksRepository): BookmarksRepository
}
