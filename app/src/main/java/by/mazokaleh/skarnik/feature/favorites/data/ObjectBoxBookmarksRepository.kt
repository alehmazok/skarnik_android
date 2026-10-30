package by.mazokaleh.skarnik.feature.favorites.data

import by.mazokaleh.skarnik.core.common.IoDispatcher
import by.mazokaleh.skarnik.core.database.SkarnikDatabase
import by.mazokaleh.skarnik.core.database.entity.ObjectboxFavoriteWord
import by.mazokaleh.skarnik.core.database.entity.ObjectboxFavoriteWord_
import by.mazokaleh.skarnik.core.model.Word
import by.mazokaleh.skarnik.feature.favorites.domain.BookmarksRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.objectbox.Box
import io.objectbox.query.Query
import javax.inject.Inject
import kotlinx.coroutines.CoroutineDispatcher
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
