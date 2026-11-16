package by.mazokaleh.skarnik.feature.translation.domain

import by.mazokaleh.skarnik.core.model.Dictionary
import by.mazokaleh.skarnik.core.model.Word

/**
 * Looks words up in the bundled search index, so a translation opened by id knows its word and which of its words
 * link to an entry or have a stress table, and the same word in the other dictionary.
 */
interface WordRepository :
    BelarusianWordIndex,
    SameWordIndex {
    /** Null when the index has no such word. */
    suspend fun find(dictionary: Dictionary, wordId: Long): Word?
}
