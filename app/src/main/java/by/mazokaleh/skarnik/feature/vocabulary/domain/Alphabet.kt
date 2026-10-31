package by.mazokaleh.skarnik.feature.vocabulary.domain

import by.mazokaleh.skarnik.core.model.Dictionary

/**
 * Letter order of a dictionary's language. Code-point order is wrong for both: ё, і and ў come after я in
 * Unicode. The order is ours rather than a [java.text.Collator]'s, so it is the same on every device and in
 * JVM tests (which have no Belarusian collation).
 */
enum class Alphabet(letters: String) {
    Belarusian("абвгґдеёжзійклмнопрстуўфхцчшыьэюя"),
    Russian("абвгдеёжзийклмнопрстуфхцчшщъыьэюя");

    private val ranks: Map<Char, Int> = letters.withIndex().associate { (index, letter) -> letter to index }

    /** The position of a lower-case [letter], or null when it is not in this alphabet. */
    fun rank(letter: Char): Int? = ranks[letter]

    /**
     * A key that sorts lower-case words in this alphabet's order with plain [String.compareTo]: each letter is
     * replaced by a private-use character in alphabet order; anything else (hyphens, apostrophes, Latin) stays
     * as it is and so sorts before letters.
     */
    fun sortKey(word: String): String = buildString(word.length) {
        word.forEach { char -> append(ranks[char]?.let { (SORT_KEY_BASE + it).toChar() } ?: char) }
    }

    private companion object {
        const val SORT_KEY_BASE = 0xE000
    }
}

val Dictionary.alphabet: Alphabet
    get() = when (this) {
        Dictionary.RusBel -> Alphabet.Russian
        Dictionary.BelRus, Dictionary.Tsbm -> Alphabet.Belarusian
    }
