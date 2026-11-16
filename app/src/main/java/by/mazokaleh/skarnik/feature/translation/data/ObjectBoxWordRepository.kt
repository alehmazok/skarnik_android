package by.mazokaleh.skarnik.feature.translation.data

import by.mazokaleh.skarnik.core.common.IoDispatcher
import by.mazokaleh.skarnik.core.database.SkarnikDatabase
import by.mazokaleh.skarnik.core.database.entity.ObjectboxSearchWord_
import by.mazokaleh.skarnik.core.model.Dictionary
import by.mazokaleh.skarnik.core.model.Word
import by.mazokaleh.skarnik.core.model.WordLink
import by.mazokaleh.skarnik.feature.translation.domain.WordRepository
import io.objectbox.query.QueryBuilder.StringOrder
import javax.inject.Inject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext

/** Flutter's `ObjectboxWordRepository`. */
class ObjectBoxWordRepository @Inject constructor(
    private val database: SkarnikDatabase,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : WordRepository {
    override suspend fun find(dictionary: Dictionary, wordId: Long): Word? = withContext(ioDispatcher) {
        database.stores().searchBox
            .query(
                ObjectboxSearchWord_.langId.equal(dictionary.langId)
                    .and(ObjectboxSearchWord_.wordId.equal(wordId))
            )
            .build()
            .use { it.findFirst() }
            ?.let {
                Word(
                    dictionary = dictionary,
                    wordId = it.wordId,
                    word = it.word,
                    letter = it.letter,
                    lword = it.lword,
                    lwordMask = it.lwordMask
                )
            }
    }

    /** One query on `lword` for all words of an entry. */
    override suspend fun belarusianWords(keys: Set<String>): Map<String, WordLink> {
        if (keys.isEmpty()) return emptyMap()
        val rows = withContext(ioDispatcher) {
            database.stores().searchBox
                .query(
                    ObjectboxSearchWord_.lword.oneOf(keys.toTypedArray(), StringOrder.CASE_SENSITIVE)
                        .and(ObjectboxSearchWord_.langId.oneOf(BELARUSIAN_LANG_IDS))
                )
                .build()
                .use { it.find() }
        }
        return rows
            .mapNotNull { row -> Dictionary.byLangId(row.langId)?.let { row.lword to WordLink(it, row.wordId) } }
            // Бел-Рус first, then the lowest id: the same entry for a word every time.
            .sortedWith(compareBy({ it.second.dictionary != Dictionary.BelRus }, { it.second.wordId }))
            .distinctBy { it.first }
            .toMap()
    }

    private companion object {
        val BELARUSIAN_LANG_IDS = longArrayOf(Dictionary.BelRus.langId, Dictionary.Tsbm.langId)
    }
}
