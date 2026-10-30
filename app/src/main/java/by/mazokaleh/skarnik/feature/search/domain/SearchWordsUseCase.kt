package by.mazokaleh.skarnik.feature.search.domain

import by.mazokaleh.skarnik.core.common.DefaultDispatcher
import javax.inject.Inject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext

data class SearchResult(val words: List<SearchWord>, val usedPrepositionFallback: Boolean)

/**
 * Word search, ported from skarnik-flutter's `ObjectboxSearchRepository`:
 *
 * 1. Lowercase the query.
 * 2. Prefix match on `lword`, then on `lwordMask`, each for the query as typed and with [LETTER_SUBSTITUTIONS]
 *    applied (a Russian keyboard can type Belarusian words). Up to [RESULT_LIMIT] words per stage.
 * 3. Nothing found and at least [FUZZY_MIN_QUERY_LENGTH] letters: Damerau–Levenshtein over the words filed
 *    under the query's first letter (distance 1 up to 5 letters, else 2).
 * 4. Still nothing: if the query starts with a one-letter preposition or conjunction, typed glued to the next
 *    word («всмысле»), run steps 2–3 once more without it.
 */
class SearchWordsUseCase @Inject constructor(
    private val index: SearchIndex,
    @DefaultDispatcher private val defaultDispatcher: CoroutineDispatcher
) {
    suspend operator fun invoke(query: String): SearchResult {
        val lowercased = query.lowercase()
        val primary = searchOnce(lowercased)
        if (primary.isNotEmpty()) return SearchResult(primary, usedPrepositionFallback = false)

        val stripped = stripGluedPrefixLetter(lowercased)
            ?: return SearchResult(primary, usedPrepositionFallback = false)
        return SearchResult(searchOnce(stripped), usedPrepositionFallback = true)
    }

    private suspend fun searchOnce(query: String): List<SearchWord> {
        val substituted = applySubstitutions(query)
        val prefixes = setOf(query, substituted)

        val byWord = index.findByWord(prefixes, RESULT_LIMIT)
        val byMask = index.findByWordMask(prefixes, byWord.mapTo(HashSet()) { it.id }, RESULT_LIMIT)
        val exact = (byWord + byMask).distinctBy { it.id }
        if (exact.isNotEmpty() || query.length < FUZZY_MIN_QUERY_LENGTH) return exact

        return fuzzySearch(substituted)
    }

    private suspend fun fuzzySearch(query: String): List<SearchWord> {
        val candidates = index.findByLetter(query.first().toString(), FUZZY_CANDIDATE_LIMIT)
        val maxDistance = if (query.length <= FUZZY_SHORT_QUERY_LENGTH) 1 else 2
        return withContext(defaultDispatcher) {
            rankFuzzyCandidates(candidates, query, maxDistance, RESULT_LIMIT)
        }
    }

    companion object {
        /** Results per stage (`AppConfig.wordsSearchLimit`). */
        const val RESULT_LIMIT = 15

        const val FUZZY_MIN_QUERY_LENGTH = 3
        private const val FUZZY_SHORT_QUERY_LENGTH = 5

        /**
         * Candidates loaded for fuzzy matching. Must cover the largest first-letter bucket («п», ~62k of 316k
         * words), or a typo's target can be cut off before it is scored.
         */
        const val FUZZY_CANDIDATE_LIMIT = 65_000

        /** Russian letters (and the ASCII apostrophe) mapped to their Belarusian counterparts. */
        val LETTER_SUBSTITUTIONS = linkedMapOf(
            "и" to "і",
            "е" to "ё",
            "щ" to "ў",
            "ъ" to "‘",
            "'" to "‘"
        )

        /**
         * Russian and Belarusian one-letter prepositions and conjunctions, taken from the dictionary's own
         * part-of-speech tags. Users often type them glued to the next word.
         */
        private val GLUED_PREFIX_LETTERS = setOf('а', 'в', 'ж', 'з', 'и', 'і', 'к', 'о', 'с', 'у', 'ў')

        fun applySubstitutions(query: String): String =
            LETTER_SUBSTITUTIONS.entries.fold(query) { acc, (from, to) -> acc.replace(from, to) }

        /** [query] without a leading glued preposition, or null. Only ever strips one letter. */
        fun stripGluedPrefixLetter(query: String): String? =
            if (query.length >= 2 && query[0] in GLUED_PREFIX_LETTERS) query.substring(1) else null
    }
}
