package by.mazokaleh.skarnik.feature.offline.data

import by.mazokaleh.skarnik.core.common.IoDispatcher
import by.mazokaleh.skarnik.core.database.SkarnikDatabase
import by.mazokaleh.skarnik.core.database.changes
import by.mazokaleh.skarnik.core.database.entity.ObjectboxTranslationWord
import by.mazokaleh.skarnik.core.database.entity.ObjectboxTranslationWord_
import by.mazokaleh.skarnik.core.model.Dictionary
import by.mazokaleh.skarnik.feature.offline.domain.DownloadedWord
import by.mazokaleh.skarnik.feature.offline.domain.OfflineDictionaryStore
import javax.inject.Inject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

/**
 * The translation store holds only downloaded dictionaries (fetched translations live in the HTTP cache), so a
 * dictionary's word count is the count of its `langId`, as in Flutter's `ObjectboxTranslationRepository`.
 */
class ObjectBoxOfflineDictionaryStore @Inject constructor(
    private val database: SkarnikDatabase,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : OfflineDictionaryStore {
    override suspend fun count(dictionary: Dictionary): Long = withContext(ioDispatcher) { countNow(dictionary) }

    override fun observeCount(dictionary: Dictionary): Flow<Long> = flow {
        val store = database.stores().translation
        emitAll(store.changes<ObjectboxTranslationWord>().map { countNow(dictionary) })
    }.distinctUntilChanged().flowOn(ioDispatcher)

    override suspend fun putAll(dictionary: Dictionary, words: List<DownloadedWord>) = withContext(ioDispatcher) {
        val langId = dictionary.langId
        val entities = words.map {
            ObjectboxTranslationWord(
                langId = langId,
                wordId = it.wordId,
                key = ObjectboxTranslationWord.keyOf(langId, it.wordId),
                stress = it.stress,
                translation = it.translation,
                redirectTo = it.redirectTo
            )
        }
        database.stores().translationBox.put(entities)
    }

    override suspend fun clear(dictionary: Dictionary) {
        withContext(ioDispatcher) {
            database.stores().translationBox
                .query(ObjectboxTranslationWord_.langId.equal(dictionary.langId))
                .build()
                .use { it.remove() }
        }
    }

    private suspend fun countNow(dictionary: Dictionary): Long = database.stores().translationBox
        .query(ObjectboxTranslationWord_.langId.equal(dictionary.langId))
        .build()
        .use { it.count() }
}
