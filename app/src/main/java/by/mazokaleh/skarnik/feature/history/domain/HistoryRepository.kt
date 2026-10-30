package by.mazokaleh.skarnik.feature.history.domain

import by.mazokaleh.skarnik.core.model.Word

fun interface HistoryRepository {
    /** Adds [word] as the newest entry; an older entry for the same word is removed. */
    suspend fun save(word: Word)
}
