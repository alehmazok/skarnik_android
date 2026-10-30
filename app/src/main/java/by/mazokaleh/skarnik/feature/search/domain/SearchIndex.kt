package by.mazokaleh.skarnik.feature.search.domain

/** Queries on the bundled search index. All queries are case-sensitive: the index is stored lowercase. */
interface SearchIndex {
    /** Words whose `lword` starts with any of [prefixes], ordered by `lword`. */
    suspend fun findByWord(prefixes: Set<String>, limit: Int): List<SearchWord>

    /** Like [findByWord], on `lwordMask` (alternative forms), leaving out [excludedIds]. */
    suspend fun findByWordMask(prefixes: Set<String>, excludedIds: Set<Long>, limit: Int): List<SearchWord>

    /** Up to [limit] words filed under [letter]: the candidates for fuzzy matching. */
    suspend fun findByLetter(letter: String, limit: Int): List<SearchWord>
}
