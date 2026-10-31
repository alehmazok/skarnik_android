package by.mazokaleh.skarnik.feature.vocabulary.domain

import by.mazokaleh.skarnik.core.common.DefaultDispatcher
import by.mazokaleh.skarnik.core.model.Dictionary
import javax.inject.Inject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext

/**
 * Loads a whole dictionary (about 100k words) and orders it alphabetically. Flutter listed the store in
 * insertion order; the order and the grouping by the stored `letter` follow the iOS app instead.
 */
class LoadVocabularyUseCase @Inject constructor(
    private val repository: VocabularyRepository,
    @DefaultDispatcher private val defaultDispatcher: CoroutineDispatcher
) {
    suspend operator fun invoke(dictionary: Dictionary): Vocabulary {
        val entries = repository.entries(dictionary)
        return withContext(defaultDispatcher) { buildVocabulary(dictionary.alphabet, entries) }
    }
}

/**
 * Sorts [entries] by [alphabet] (equal words by id, so the order is stable) and groups them by their stored
 * first letter: letters of the alphabet in its order, then any other letters by code point.
 */
internal fun buildVocabulary(alphabet: Alphabet, entries: List<VocabularyEntry>): Vocabulary {
    val byLetter = entries
        .map { it to alphabet.sortKey(it.lword) }
        .sortedWith(compareBy<Pair<VocabularyEntry, String>> { it.second }.thenBy { it.first.wordId })
        .groupBy(keySelector = { it.first.letter.lowercase() }, valueTransform = { it.first })
    val letters = byLetter.keys.sortedWith(
        compareBy<String> { it.alphabetRank(alphabet) ?: Int.MAX_VALUE }.thenBy { it }
    )
    var row = 0
    val sections = letters.map { letter ->
        val words = byLetter.getValue(letter).map { VocabularyWord(it.wordId, it.word) }
        VocabularySection(
            letter = letter.uppercase(),
            words = words,
            firstRow = row,
            inIndex = letter.alphabetRank(alphabet) != null
        ).also { row += 1 + words.size }
    }
    return Vocabulary(sections)
}

private fun String.alphabetRank(alphabet: Alphabet): Int? = singleOrNull()?.let(alphabet::rank)
