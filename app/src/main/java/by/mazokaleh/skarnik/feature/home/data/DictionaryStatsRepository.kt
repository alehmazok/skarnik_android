package by.mazokaleh.skarnik.feature.home.data

import by.mazokaleh.skarnik.core.common.IoDispatcher
import by.mazokaleh.skarnik.core.database.SkarnikDatabase
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import javax.inject.Inject

interface DictionaryStatsRepository {
    /** Number of words in the bundled search index. Opens the stores on first call. */
    suspend fun searchWordCount(): Long
}

class ObjectBoxDictionaryStatsRepository @Inject constructor(
    private val database: SkarnikDatabase,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : DictionaryStatsRepository {
    override suspend fun searchWordCount(): Long = withContext(ioDispatcher) {
        database.stores().searchBox.count()
    }
}

@Module
@InstallIn(SingletonComponent::class)
interface DictionaryStatsModule {
    @Binds
    fun bindDictionaryStatsRepository(impl: ObjectBoxDictionaryStatsRepository): DictionaryStatsRepository
}
