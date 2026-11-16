package by.mazokaleh.skarnik.feature.translation.data

import by.mazokaleh.skarnik.core.common.IoDispatcher
import by.mazokaleh.skarnik.core.database.SkarnikDatabase
import by.mazokaleh.skarnik.core.database.entity.ObjectboxSearchWord_
import by.mazokaleh.skarnik.core.model.Dictionary
import by.mazokaleh.skarnik.core.model.Word
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

    /** One query on `lword` for all candidates of an entry. */
    override suspend fun knownWords(lwords: Set<String>): Set<String> {
        if (lwords.isEmpty()) return emptySet()
        return withContext(ioDispatcher) {
            database.stores().searchBox
                .query(
                    ObjectboxSearchWord_.lword.oneOf(lwords.toTypedArray(), StringOrder.CASE_SENSITIVE)
                        .and(ObjectboxSearchWord_.langId.oneOf(STRESS_LANG_IDS))
                )
                .build()
                .use { query -> query.property(ObjectboxSearchWord_.lword).distinct().findStrings() }
                ?.toSet()
                .orEmpty()
        }
    }

    private companion object {
        val STRESS_LANG_IDS = longArrayOf(Dictionary.BelRus.langId, Dictionary.Tsbm.langId)
    }
}
