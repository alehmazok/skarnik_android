package by.mazokaleh.skarnik.feature.search.domain

import by.mazokaleh.skarnik.core.model.Dictionary

/** A word from the search index. [id] is the index row id; [wordId] is the dictionary's own word id. */
data class SearchWord(
    val id: Long,
    val dictionary: Dictionary,
    val wordId: Long,
    val word: String,
    val letter: String,
    val lword: String
)
