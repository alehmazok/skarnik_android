package by.mazokaleh.skarnik.feature.widget.domain

import by.mazokaleh.skarnik.core.model.Dictionary
import by.mazokaleh.skarnik.core.model.Word

/** The Бел-Рус word the «Слова дня» widget shows, with its translation as plain text. */
data class WordOfTheDay(val wordId: Long, val word: String, val translation: String) {
    /** The word's link, which the widget opens; null for the fallback word, which is not in the dictionary. */
    val link: String? get() = if (wordId > 0) "https://skarnik.app/r/${Dictionary.BelRus.path}/$wordId" else null

    companion object {
        /** Shown until the first refresh succeeds; the same word as in Flutter. */
        val Fallback = WordOfTheDay(wordId = -1, word = "халэмус", translation = "гибель, конец")
    }
}

/** The word the widget shows now. */
interface WordOfTheDayStore {
    /** Null before the first refresh. */
    suspend fun current(): WordOfTheDay?

    suspend fun save(word: WordOfTheDay)
}

/** Picks words for the widget. */
fun interface RandomWordSource {
    /** A random Бел-Рус word of the bundled index, or null when the index has none. */
    suspend fun randomBelRusWord(): Word?
}
