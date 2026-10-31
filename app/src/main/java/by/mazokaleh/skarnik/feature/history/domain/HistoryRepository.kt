package by.mazokaleh.skarnik.feature.history.domain

import by.mazokaleh.skarnik.core.model.SavedWordsPage
import by.mazokaleh.skarnik.core.model.Word
import kotlinx.coroutines.flow.Flow

interface HistoryRepository {
    /** Adds [word] as the newest entry; an older entry for the same word is removed. */
    suspend fun save(word: Word)

    /** Removes every history entry; bookmarks are kept (Flutter's `ObjectboxSettingsHistoryRepository`). */
    suspend fun clear()

    /** The newest [limit] entries, newest first, re-read whenever history changes. */
    fun observe(limit: Int): Flow<SavedWordsPage>
}
