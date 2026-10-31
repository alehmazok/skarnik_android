package by.mazokaleh.skarnik.feature.vocabulary.data

import by.mazokaleh.skarnik.core.common.IoDispatcher
import by.mazokaleh.skarnik.core.database.SkarnikDatabase
import by.mazokaleh.skarnik.core.database.entity.ObjectboxSearchWord_
import by.mazokaleh.skarnik.core.model.Dictionary
import by.mazokaleh.skarnik.feature.vocabulary.domain.VocabularyEntry
import by.mazokaleh.skarnik.feature.vocabulary.domain.VocabularyRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Inject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext

/** Reads a dictionary from the search store, the same source as Flutter's `ObjectboxVocabularyRepository`. */
class ObjectBoxVocabularyRepository @Inject constructor(
    private val database: SkarnikDatabase,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : VocabularyRepository {
    override suspend fun entries(dictionary: Dictionary): List<VocabularyEntry> = withContext(ioDispatcher) {
        val entries = ArrayList<VocabularyEntry>()
        database.stores().searchBox
            .query(ObjectboxSearchWord_.langId.equal(dictionary.langId))
            .build()
            .use { query ->
                // One entity at a time: the ~100k entities are never held in memory together.
                query.forEach { entries += VocabularyEntry(it.wordId, it.word, it.letter, it.lword) }
            }
        entries
    }
}

@Module
@InstallIn(SingletonComponent::class)
interface VocabularyModule {
    @Binds
    fun bindVocabularyRepository(impl: ObjectBoxVocabularyRepository): VocabularyRepository
}
