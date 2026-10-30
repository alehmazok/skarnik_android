package by.mazokaleh.skarnik.feature.translation.data

import by.mazokaleh.skarnik.core.common.IoDispatcher
import by.mazokaleh.skarnik.core.database.SkarnikDatabase
import by.mazokaleh.skarnik.core.database.entity.ObjectboxSearchWord_
import by.mazokaleh.skarnik.core.model.Dictionary
import by.mazokaleh.skarnik.core.model.Word
import by.mazokaleh.skarnik.feature.translation.domain.WordRepository
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
}
