package by.mazokaleh.skarnik.feature.offline.domain

import by.mazokaleh.skarnik.core.model.Dictionary

/** One `main_word` row of a downloaded dictionary. [rowId] is the row's primary key, used as the resume cursor. */
data class DownloadedWord(
    val rowId: Long,
    val wordId: Long,
    val stress: String?,
    val translation: String,
    val redirectTo: String?
)

/** Where whole dictionaries are downloaded from (Supabase `main_word`, as in Flutter). */
interface OfflineDictionarySource {
    /** How many words [dictionary] has. */
    suspend fun countWords(dictionary: Dictionary): Long

    /**
     * The next words of [dictionary] with a row id above [afterRowId], in row id order, at most [limit]
     * (the server may return fewer). Empty when there are no more.
     */
    suspend fun fetchPage(dictionary: Dictionary, afterRowId: Long, limit: Int): List<DownloadedWord>
}
