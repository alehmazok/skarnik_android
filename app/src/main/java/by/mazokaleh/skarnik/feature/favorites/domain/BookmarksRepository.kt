package by.mazokaleh.skarnik.feature.favorites.domain

import by.mazokaleh.skarnik.core.model.Word

interface BookmarksRepository {
    suspend fun isBookmarked(word: Word): Boolean

    /** Adds [word] as the newest bookmark (an existing one moves to the top, as in Flutter). */
    suspend fun add(word: Word)

    suspend fun remove(word: Word)
}
