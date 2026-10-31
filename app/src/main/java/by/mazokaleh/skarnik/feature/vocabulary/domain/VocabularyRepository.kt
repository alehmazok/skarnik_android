package by.mazokaleh.skarnik.feature.vocabulary.domain

import by.mazokaleh.skarnik.core.model.Dictionary

interface VocabularyRepository {
    /** Every word of [dictionary] in the bundled index, in no particular order. */
    suspend fun entries(dictionary: Dictionary): List<VocabularyEntry>
}
