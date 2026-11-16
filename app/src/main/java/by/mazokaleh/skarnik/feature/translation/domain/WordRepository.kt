package by.mazokaleh.skarnik.feature.translation.domain

import by.mazokaleh.skarnik.core.model.Dictionary
import by.mazokaleh.skarnik.core.model.Word
import by.mazokaleh.skarnik.feature.stress.domain.StressWordIndex

/**
 * Looks words up in the bundled search index, so a translation opened by id knows its word and which of its words
 * can have a stress table.
 */
interface WordRepository : StressWordIndex {
    /** Null when the index has no such word. */
    suspend fun find(dictionary: Dictionary, wordId: Long): Word?
}
