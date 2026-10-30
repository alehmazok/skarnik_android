package by.mazokaleh.skarnik.core.model

/** A word of the bundled index, as history and bookmarks store it. */
data class Word(
    val dictionary: Dictionary,
    val wordId: Long,
    val word: String,
    val letter: String,
    val lword: String,
    val lwordMask: String?
)
