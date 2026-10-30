package by.mazokaleh.skarnik.feature.favorites.domain

import by.mazokaleh.skarnik.core.model.SavedWord
import by.mazokaleh.skarnik.core.model.SavedWordsPage
import by.mazokaleh.skarnik.core.model.Word
import kotlinx.coroutines.flow.Flow

interface BookmarksRepository {
    suspend fun isBookmarked(word: Word): Boolean

    /** Adds [word] as the newest bookmark (an existing one moves to the top, as in Flutter). */
    suspend fun add(word: Word)

    suspend fun remove(word: Word)

    /** The first [limit] bookmarks in [sortOrder], re-read whenever bookmarks change. */
    fun observe(sortOrder: BookmarksSortOrder, limit: Int): Flow<SavedWordsPage>

    /** Removes a listed bookmark; [restore] can put it back. */
    suspend fun remove(bookmark: SavedWord)

    /** Puts a removed bookmark back with its old id, so it keeps its place in the date order. */
    suspend fun restore(bookmark: SavedWord)
}
