package by.mazokaleh.skarnik.feature.translation.domain

import by.mazokaleh.skarnik.core.model.Word

data class Translation(val word: Word, val stress: String?, val html: String, val source: Source) {
    /** The word with its stress mark when the entry has one. */
    val title: String get() = stress ?: word.word

    /** The link shared from the app; skarnik.app renders it for people without the app. */
    val shareUrl: String get() = "https://skarnik.app/r/${word.dictionary.path}/${word.wordId}"

    enum class Source { Local, Api, Cloud }
}
