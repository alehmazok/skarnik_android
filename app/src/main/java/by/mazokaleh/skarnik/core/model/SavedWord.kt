package by.mazokaleh.skarnik.core.model

/** A history or bookmark entry. [id] is the row id, which also orders the list by date. */
data class SavedWord(val id: Long, val dictionary: Dictionary, val wordId: Long, val word: String, val letter: String)

/** The first rows of a list; [hasMore] tells whether rows follow. */
data class SavedWordsPage(val words: List<SavedWord>, val hasMore: Boolean)
