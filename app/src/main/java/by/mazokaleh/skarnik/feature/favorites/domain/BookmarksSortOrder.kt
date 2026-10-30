package by.mazokaleh.skarnik.feature.favorites.domain

import kotlinx.coroutines.flow.Flow

/** [key] is the value Flutter stored under `favorites_sort_order`, kept so the setting migrates as is. */
enum class BookmarksSortOrder(val key: String) {
    DateAdded("dateAdded"),
    Alphabetical("alphabetical");

    companion object {
        val Default = DateAdded

        fun byKey(key: String?): BookmarksSortOrder = entries.firstOrNull { it.key == key } ?: Default
    }
}

interface BookmarksSortRepository {
    val sortOrder: Flow<BookmarksSortOrder>

    suspend fun setSortOrder(sortOrder: BookmarksSortOrder)
}
