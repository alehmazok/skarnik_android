package by.mazokaleh.skarnik.feature.search.domain

import kotlin.math.abs

/**
 * Damerau–Levenshtein (optimal string alignment) distance between [a] and [b]: insertions, deletions,
 * substitutions and adjacent transpositions each count as one edit. Case-sensitive; callers lowercase first.
 *
 * Ported from skarnik-flutter's `damerau_levenshtein.dart`.
 */
fun damerauLevenshteinDistance(a: String, b: String): Int {
    if (a.isEmpty() || b.isEmpty()) return maxOf(a.length, b.length)

    val distances = Array(a.length + 1) { IntArray(b.length + 1) }
    for (i in 0..a.length) distances[i][0] = i
    for (j in 0..b.length) distances[0][j] = j

    for (i in 1..a.length) {
        for (j in 1..b.length) {
            val cost = if (a[i - 1] == b[j - 1]) 0 else 1
            var value = minOf(
                distances[i - 1][j] + 1,
                distances[i][j - 1] + 1,
                distances[i - 1][j - 1] + cost
            )
            if (isTransposition(a, b, i, j)) {
                value = minOf(value, distances[i - 2][j - 2] + cost)
            }
            distances[i][j] = value
        }
    }
    return distances[a.length][b.length]
}

/** The last two letters of `a[0, i)` are the last two of `b[0, j)` swapped. */
private fun isTransposition(a: String, b: String, i: Int, j: Int): Boolean {
    if (i < 2 || j < 2) return false
    return a[i - 1] == b[j - 2] && a[i - 2] == b[j - 1]
}

/**
 * Keeps the [candidates] within [maxDistance] edits of [query], closest first and then alphabetically by
 * `lword`, at most [limit] of them. Ported from `fuzzy_candidate_ranking.dart`.
 */
fun rankFuzzyCandidates(
    candidates: List<SearchWord>,
    query: String,
    maxDistance: Int,
    limit: Int
): List<SearchWord> = candidates
    .asSequence()
    // The distance is at least the length difference, so skip the matrix for words that cannot match.
    .filter { abs(it.lword.length - query.length) <= maxDistance }
    .map { it to damerauLevenshteinDistance(query, it.lword) }
    .filter { (_, distance) -> distance <= maxDistance }
    .sortedWith(compareBy<Pair<SearchWord, Int>> { it.second }.thenBy { it.first.lword })
    .take(limit)
    .map { it.first }
    .toList()
