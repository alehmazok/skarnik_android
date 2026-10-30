package by.mazokaleh.skarnik.feature.translation.data

import by.mazokaleh.skarnik.core.common.IoDispatcher
import by.mazokaleh.skarnik.core.database.SkarnikDatabase
import by.mazokaleh.skarnik.core.database.entity.ObjectboxTranslationWord
import by.mazokaleh.skarnik.core.database.entity.ObjectboxTranslationWord_
import by.mazokaleh.skarnik.core.model.Dictionary
import by.mazokaleh.skarnik.feature.translation.domain.LocalTranslationSource
import by.mazokaleh.skarnik.feature.translation.domain.TranslationEntry
import io.objectbox.query.QueryBuilder.StringOrder
import javax.inject.Inject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext

/** Offline dictionaries in the translation store (Flutter's `ObjectboxTranslationRepository`). */
class ObjectBoxLocalTranslationSource @Inject constructor(
    private val database: SkarnikDatabase,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : LocalTranslationSource {
    override suspend fun find(dictionary: Dictionary, wordId: Long): TranslationEntry? = withContext(ioDispatcher) {
        val key = ObjectboxTranslationWord.keyOf(dictionary.langId, wordId)
        database.stores().translationBox
            .query(ObjectboxTranslationWord_.key.equal(key, StringOrder.CASE_SENSITIVE))
            .build()
            .use { it.findFirst() }
            ?.let { TranslationEntry(stress = it.stress, html = it.translation, redirectTo = it.redirectTo) }
    }
}
