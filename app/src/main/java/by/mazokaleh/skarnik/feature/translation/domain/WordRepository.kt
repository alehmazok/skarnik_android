package by.mazokaleh.skarnik.feature.translation.domain

import by.mazokaleh.skarnik.core.model.Dictionary
import by.mazokaleh.skarnik.core.model.Word

/** Looks words up in the bundled search index, so a translation opened by id knows its word. */
fun interface WordRepository {
    /** Null when the index has no such word. */
    suspend fun find(dictionary: Dictionary, wordId: Long): Word?
}
