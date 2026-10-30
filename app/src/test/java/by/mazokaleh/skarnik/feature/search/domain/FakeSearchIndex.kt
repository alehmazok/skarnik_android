package by.mazokaleh.skarnik.feature.search.domain

import by.mazokaleh.skarnik.core.model.Dictionary

/** In-memory [SearchIndex] with the same matching rules as ObjectBox; records the letters fuzzy search asked for. */
class FakeSearchIndex(private val rows: List<Row>) : SearchIndex {
    data class Row(val word: SearchWord, val mask: String? = null)

    val prefixQueries = mutableListOf<Set<String>>()
    val fuzzyLetters = mutableListOf<String>()

    override suspend fun findByWord(prefixes: Set<String>, limit: Int): List<SearchWord> {
        prefixQueries += prefixes
        return rows.map { it.word }
            .filter { word -> prefixes.any { word.lword.startsWith(it) } }
            .sortedBy { it.lword }
            .take(limit)
    }

    override suspend fun findByWordMask(prefixes: Set<String>, excludedIds: Set<Long>, limit: Int): List<SearchWord> =
        rows.filter { row -> row.word.id !in excludedIds && prefixes.any { row.mask?.startsWith(it) == true } }
            .sortedBy { it.mask }
            .take(limit)
            .map { it.word }

    override suspend fun findByLetter(letter: String, limit: Int): List<SearchWord> {
        fuzzyLetters += letter
        return rows.map { it.word }.filter { it.letter == letter }.take(limit)
    }

    companion object {
        private var nextId = 1L

        fun row(lword: String, mask: String? = null, dictionary: Dictionary = Dictionary.BelRus): Row {
            val id = nextId++
            val word = SearchWord(id, dictionary, wordId = id, word = lword, letter = lword.take(1), lword = lword)
            return Row(word, mask)
        }
    }
}
