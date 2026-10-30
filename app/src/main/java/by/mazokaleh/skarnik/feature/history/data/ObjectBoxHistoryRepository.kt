package by.mazokaleh.skarnik.feature.history.data

import by.mazokaleh.skarnik.core.common.IoDispatcher
import by.mazokaleh.skarnik.core.database.SkarnikDatabase
import by.mazokaleh.skarnik.core.database.entity.ObjectboxHistoryWord
import by.mazokaleh.skarnik.core.database.entity.ObjectboxHistoryWord_
import by.mazokaleh.skarnik.core.model.Word
import by.mazokaleh.skarnik.feature.history.domain.HistoryRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Inject
import kotlinx.coroutines.CoroutineDispatcher
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
}

@Module
@InstallIn(SingletonComponent::class)
interface HistoryModule {
    @Binds
    fun bindHistoryRepository(impl: ObjectBoxHistoryRepository): HistoryRepository
}
