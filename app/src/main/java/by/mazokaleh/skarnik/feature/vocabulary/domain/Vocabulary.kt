package by.mazokaleh.skarnik.feature.vocabulary.domain

/** A word of the bundled index as the store holds it, before sorting. */
data class VocabularyEntry(val wordId: Long, val word: String, val letter: String, val lword: String)

/** A row of the dictionary list. */
data class VocabularyWord(val wordId: Long, val word: String)

/**
 * The words that start with [letter] (upper case). [firstRow] is the position of the section's header in the
 * flat list (headers and words), where the letter index jumps to. [inIndex] is false for a letter outside the
 * dictionary's alphabet: its words are listed at the end but get no index entry.
 */
data class VocabularySection(
    val letter: String,
    val words: List<VocabularyWord>,
    val firstRow: Int,
    val inIndex: Boolean
)

/** A whole dictionary, in alphabetical order. */
data class Vocabulary(val sections: List<VocabularySection>) {
    val indexSections: List<VocabularySection> = sections.filter { it.inIndex }
}
