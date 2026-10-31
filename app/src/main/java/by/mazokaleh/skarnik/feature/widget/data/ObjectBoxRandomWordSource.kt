package by.mazokaleh.skarnik.feature.widget.data

import by.mazokaleh.skarnik.core.common.IoDispatcher
import by.mazokaleh.skarnik.core.database.SkarnikDatabase
import by.mazokaleh.skarnik.core.database.entity.ObjectboxSearchWord_
import by.mazokaleh.skarnik.core.model.Dictionary
import by.mazokaleh.skarnik.core.model.Word
import by.mazokaleh.skarnik.feature.widget.domain.RandomWordSource
import javax.inject.Inject
import kotlin.random.Random
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext

/** Flutter's `PickRandomWordUseCase`: a random offset into the Бел-Рус words of the search index. */
class ObjectBoxRandomWordSource(
    private val database: SkarnikDatabase,
    private val ioDispatcher: CoroutineDispatcher,
    private val random: Random
) : RandomWordSource {
    @Inject
    constructor(database: SkarnikDatabase, @IoDispatcher ioDispatcher: CoroutineDispatcher) :
        this(database, ioDispatcher, Random.Default)

    override suspend fun randomBelRusWord(): Word? = withContext(ioDispatcher) {
        database.stores().searchBox
            .query(ObjectboxSearchWord_.langId.equal(Dictionary.BelRus.langId))
            .build()
            .use { query ->
                val total = query.count()
                if (total == 0L) return@use null
                query.find(random.nextLong(total), 1).firstOrNull()
            }
            ?.let {
                Word(
                    dictionary = Dictionary.BelRus,
                    wordId = it.wordId,
                    word = it.word,
                    letter = it.letter,
                    lword = it.lword,
                    lwordMask = it.lwordMask
                )
            }
    }
}
