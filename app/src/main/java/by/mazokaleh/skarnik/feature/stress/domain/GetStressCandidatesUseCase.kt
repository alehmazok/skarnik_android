package by.mazokaleh.skarnik.feature.stress.domain

import by.mazokaleh.skarnik.core.common.suspendRunCatching
import by.mazokaleh.skarnik.core.crash.logError
import by.mazokaleh.skarnik.core.model.Dictionary
import by.mazokaleh.skarnik.core.model.Word
import by.mazokaleh.skarnik.feature.translation.domain.BelarusianWordIndex
import by.mazokaleh.skarnik.feature.translation.domain.belarusianWordsIn
import by.mazokaleh.skarnik.feature.translation.domain.isSingleWord
import by.mazokaleh.skarnik.feature.translation.domain.wordKey
import javax.inject.Inject

/**
 * The words a translation offers stress tables for; empty means there is no «На́ціск» button. Same rules as
 * skarnik_ios (`SKSkarnikTranslation.belWords`):
 * - ТСБМ: the headword, unless it is a phrase.
 * - Бел-Рус: the headword, unless it is a phrase, plus the Belarusian words of the entry ([belarusianWordsIn]).
 * - Рус-Бел: the Belarusian words of the entry.
 *
 * Words taken from the HTML are kept only when the index has them as a Бел-Рус or ТСБМ headword, so inflected
 * forms («цельпука») and Russian words get no button that would lead to «не знойдзена». The list is sorted
 * ignoring case.
 */
class GetStressCandidatesUseCase @Inject constructor(private val index: BelarusianWordIndex) {
    suspend operator fun invoke(word: Word, html: String): List<String> {
        val headword = word.word.takeIf { word.dictionary != Dictionary.RusBel && it.isSingleWord() }
        val fromHtml = belarusianWordsIn(word.dictionary, html).filter { it != headword }
        val known = if (fromHtml.isEmpty()) {
            emptySet()
        } else {
            suspendRunCatching { index.belarusianWords(fromHtml.mapTo(mutableSetOf(), ::wordKey)).keys }
                .getOrElse {
                    logError(TAG, "Cannot check stress candidates against the index", it)
                    emptySet()
                }
        }
        return (listOfNotNull(headword) + fromHtml.filter { wordKey(it) in known })
            .sortedWith(compareBy { it.lowercase() })
    }

    private companion object {
        const val TAG = "StressCandidates"
    }
}
