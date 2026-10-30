package by.mazokaleh.skarnik.feature.history.data

import by.mazokaleh.skarnik.core.common.IoDispatcher
import by.mazokaleh.skarnik.core.database.SkarnikDatabase
import by.mazokaleh.skarnik.core.database.changes
import by.mazokaleh.skarnik.core.database.entity.ObjectboxHistoryWord
import by.mazokaleh.skarnik.core.database.entity.ObjectboxHistoryWord_
import by.mazokaleh.skarnik.core.model.Dictionary
import by.mazokaleh.skarnik.core.model.SavedWord
import by.mazokaleh.skarnik.core.model.SavedWordsPage
import by.mazokaleh.skarnik.core.model.Word
import by.mazokaleh.skarnik.feature.history.domain.HistoryRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.objectbox.query.QueryBuilder
import javax.inject.Inject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

/** Flutter's `ObjectboxHistoryRepository`: history is ordered by id, so a re-opened word moves to the top. */
class ObjectBoxHistoryRepository @Inject constructor(
    private val database: SkarnikDatabase,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : HistoryRepository {
    override suspend fun save(word: Word) = withContext(ioDispatcher) {
        val stores = database.stores()
        val box = stores.historyBox
        stores.history.runInTx {
            box.query(
                ObjectboxHistoryWord_.wordId.equal(word.wordId)
                    .and(ObjectboxHistoryWord_.langId.equal(word.dictionary.langId))
            ).build().use { it.remove() }
            box.put(
                ObjectboxHistoryWord(
                    langId = word.dictionary.langId,
                    letter = word.letter,
                    wordId = word.wordId,
                    word = word.word,
                    lword = word.lword,
                    lwordMask = word.lwordMask
                )
            )
        }
    }

    override fun observe(limit: Int): Flow<SavedWordsPage> = flow {
        val stores = database.stores()
        emitAll(
            stores.history.changes<ObjectboxHistoryWord>().map {
                // A query is not shared between threads, so each read builds its own.
                val rows = stores.historyBox.query().order(ObjectboxHistoryWord_.id, QueryBuilder.DESCENDING).build()
                    .use { it.find(0, limit + 1L) }
                // The extra row tells whether there is a next page.
                SavedWordsPage(rows.take(limit).mapNotNull { it.toSavedWord() }, hasMore = rows.size > limit)
            }
        )
    }.flowOn(ioDispatcher)

    private fun ObjectboxHistoryWord.toSavedWord(): SavedWord? = Dictionary.byLangId(langId)?.let {
        SavedWord(id = id, dictionary = it, wordId = wordId, word = word, letter = letter)
    }
}

@Module
@InstallIn(SingletonComponent::class)
interface HistoryModule {
    @Binds
    fun bindHistoryRepository(impl: ObjectBoxHistoryRepository): HistoryRepository
}
