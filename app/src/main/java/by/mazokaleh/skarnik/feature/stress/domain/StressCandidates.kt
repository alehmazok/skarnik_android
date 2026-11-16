package by.mazokaleh.skarnik.feature.stress.domain

import by.mazokaleh.skarnik.core.common.suspendRunCatching
import by.mazokaleh.skarnik.core.crash.logError
import by.mazokaleh.skarnik.core.model.Dictionary
import by.mazokaleh.skarnik.core.model.Word
import javax.inject.Inject
import org.jsoup.Jsoup
import org.jsoup.nodes.Document

/** The Бел-Рус and ТСБМ words of the bundled index, the only words a stress table can exist for. */
fun interface StressWordIndex {
    /** The subset of [lwords] (lowercase) that are Бел-Рус or ТСБМ headwords. */
    suspend fun knownWords(lwords: Set<String>): Set<String>
}

/**
 * The words a translation offers stress tables for; empty means there is no «На́ціск» button. Same rules as
 * skarnik_ios (`SKSkarnikTranslation.belWords`):
 * - ТСБМ: the headword, unless it is a phrase.
 * - Бел-Рус: the headword, unless it is a phrase, plus the words of the entry's headword-coloured runs (synonyms).
 * - Рус-Бел: the entry's `data-word` attributes (set by skarnik_admin's `find_inner_stress`); without them, the
 *   words of its headword-coloured runs.
 *
 * Words taken from the HTML are kept only when the index has them as a Бел-Рус or ТСБМ headword, so inflected
 * forms («цельпука») and Russian words get no button that would lead to «не знойдзена». The list is sorted
 * ignoring case.
 */
class GetStressCandidatesUseCase @Inject constructor(private val index: StressWordIndex) {
    suspend operator fun invoke(word: Word, html: String): List<String> {
        val headword = word.word.takeIf { word.dictionary != Dictionary.RusBel && it.isSingleWord() }
        val fromHtml = htmlCandidates(word.dictionary, html).filter { it != headword }
        val known = if (fromHtml.isEmpty()) {
            emptySet()
        } else {
            suspendRunCatching { index.knownWords(fromHtml.mapTo(mutableSetOf()) { it.lowercase() }) }
                .getOrElse {
                    logError(TAG, "Cannot check stress candidates against the index", it)
                    emptySet()
                }
        }
        return (listOfNotNull(headword) + fromHtml.filter { it.lowercase() in known })
            .sortedWith(compareBy { it.lowercase() })
    }

    private companion object {
        const val TAG = "StressCandidates"
    }
}

/** The single words of [html] that may have a stress table, de-duplicated, before the index check. */
internal fun htmlCandidates(dictionary: Dictionary, html: String): List<String> = when (dictionary) {
    Dictionary.Tsbm -> emptyList()
    Dictionary.BelRus -> headwordColoured(Jsoup.parseBodyFragment(html))
    Dictionary.RusBel -> Jsoup.parseBodyFragment(html).let { doc -> dataWords(doc).ifEmpty { headwordColoured(doc) } }
}

private fun dataWords(doc: Document): List<String> = doc.select("[data-word]")
    .map { it.attr("data-word").trim() }
    .filter { it.isNotEmpty() && it.isSingleWord() }
    .distinct()

/**
 * `<font color="#831b03">` (scraped skarnik.by markup) or `<font|span style="color: #831b03">` (entries edited
 * in skarnik_admin's TinyMCE), split at commas. `text()` turns `&nbsp;` into a space, so a phrase joined by
 * `&nbsp;` is not offered as one word.
 */
private fun headwordColoured(doc: Document): List<String> = doc.select("font, span")
    .filter { element ->
        element.attr("color").trim().trim('#').equals(HEADWORD_COLOR, ignoreCase = true) ||
            HEADWORD_STYLE.containsMatchIn(element.attr("style"))
    }
    .flatMap { it.text().split(',') }
    .map { it.trim() }
    .filter { it.isNotEmpty() && it.isSingleWord() }
    .distinct()

private fun String.isSingleWord() = none { it.isWhitespace() }

private const val HEADWORD_COLOR = "831b03"
private val HEADWORD_STYLE = Regex("color:\\s*#?831b03", RegexOption.IGNORE_CASE)
