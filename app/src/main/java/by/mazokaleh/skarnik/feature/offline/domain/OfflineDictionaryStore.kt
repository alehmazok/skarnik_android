package by.mazokaleh.skarnik.feature.offline.domain

import by.mazokaleh.skarnik.core.model.Dictionary
import kotlinx.coroutines.flow.Flow

/** Downloaded dictionaries in the translation store. */
interface OfflineDictionaryStore {
    suspend fun count(dictionary: Dictionary): Long

    /** The word count of [dictionary], now and after every change to the store. */
    fun observeCount(dictionary: Dictionary): Flow<Long>

    /** Writes [words] in one transaction; a word already stored is replaced. */
    suspend fun putAll(dictionary: Dictionary, words: List<DownloadedWord>)

    suspend fun clear(dictionary: Dictionary)
}
