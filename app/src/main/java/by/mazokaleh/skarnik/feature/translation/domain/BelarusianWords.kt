package by.mazokaleh.skarnik.feature.translation.domain

import by.mazokaleh.skarnik.core.common.suspendRunCatching
import by.mazokaleh.skarnik.core.crash.logError
import by.mazokaleh.skarnik.core.model.Dictionary
import by.mazokaleh.skarnik.core.model.Word
import by.mazokaleh.skarnik.core.model.WordLink
import javax.inject.Inject
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element

/** The Бел-Рус and ТСБМ headwords of the bundled index: the words a translation links to and stress tables exist for. */
fun interface BelarusianWordIndex {
    /**
     * The entry each of [keys] ([wordKey]s) is a headword of: its Бел-Рус entry, else its ТСБМ entry; the lowest id
     * when several entries share the text. Keys the index does not have are left out.
     */
    suspend fun belarusianWords(keys: Set<String>): Map<String, WordLink>
}

/** How a word is looked up in the index: lowercase, without the stress mark admin-edited entries show («я́нтар»). */
fun wordKey(text: String): String = text.replace(STRESS_MARK, "").lowercase()

/**
 * The Belarusian single words in a Бел-Рус or Рус-Бел translation, de-duplicated, before the index check (ТСБМ: none):
 * - `<font color="#831b03">` (scraped skarnik.by markup) or `<font|span style="color: #831b03">` (entries edited in
 *   skarnik_admin's TinyMCE), split at commas;
 * - for Рус-Бел, the entry's `data-word` attributes instead, when it has any (skarnik_admin's `find_inner_stress`).
 *
 * `text()` turns `&nbsp;` into a space, so a phrase joined by `&nbsp;` is not taken as one word. Same rules as
 * skarnik_ios (`SKSkarnikTranslation.belWords`).
 */
fun belarusianWordsIn(dictionary: Dictionary, html: String): List<String> = when (dictionary) {
    Dictionary.Tsbm -> emptyList()
    Dictionary.BelRus -> headwordColoured(Jsoup.parseBodyFragment(html))
    Dictionary.RusBel -> Jsoup.parseBodyFragment(html).let { doc -> dataWords(doc).ifEmpty { headwordColoured(doc) } }
}

/** True for a `<font>` or `<span>` in the headword colour. The translation renderer links the words inside it. */
fun Element.isHeadwordColoured(): Boolean {
    if (normalName() !in HEADWORD_TAGS) return false
    return attr("color").trim().trim('#').equals(HEADWORD_COLOR, ignoreCase = true) ||
        HEADWORD_STYLE.containsMatchIn(attr("style"))
}

fun String.isSingleWord(): Boolean = isNotEmpty() && none { it.isWhitespace() }

/** The comma-separated single words of a headword-coloured run, with their offsets in [text]. */
fun headwordRunParts(text: String): List<Pair<IntRange, String>> {
    val parts = mutableListOf<Pair<IntRange, String>>()
    var start = 0
    while (start <= text.length) {
        val comma = text.indexOf(',', start).let { if (it < 0) text.length else it }
        val raw = text.substring(start, comma)
        val word = raw.trim()
        if (word.isSingleWord()) {
            val from = start + raw.indexOf(word)
            parts += (from until from + word.length) to word
        }
        start = comma + 1
    }
    return parts
}

/**
 * The words of a translation that open their own entry when tapped: the Belarusian words of [belarusianWordsIn] that
 * the index has, by [wordKey]. The entry itself is left out. Empty when the index cannot be read.
 */
class FindWordLinksUseCase @Inject constructor(private val index: BelarusianWordIndex) {
    suspend operator fun invoke(word: Word, html: String): Map<String, WordLink> {
        val keys = belarusianWordsIn(word.dictionary, html).mapTo(mutableSetOf(), ::wordKey)
        if (keys.isEmpty()) return emptyMap()
        val self = WordLink(word.dictionary, word.wordId)
        return suspendRunCatching { index.belarusianWords(keys) }
            .getOrElse {
                logError(TAG, "Cannot look up the words of a translation", it)
                emptyMap()
            }
            .filterValues { it != self }
    }

    private companion object {
        const val TAG = "WordLinks"
    }
}

private fun dataWords(doc: Document): List<String> = doc.select("[data-word]")
    .map { it.attr("data-word").trim() }
    .filter { it.isSingleWord() }
    .distinct()

private fun headwordColoured(doc: Document): List<String> = doc.select("font, span")
    .filter { it.isHeadwordColoured() }
    .flatMap { it.text().split(',') }
    .map { it.trim() }
    .filter { it.isSingleWord() }
    .distinct()

private val HEADWORD_TAGS = setOf("font", "span")
private const val HEADWORD_COLOR = "831b03"
private val HEADWORD_STYLE = Regex("color:\\s*#?831b03", RegexOption.IGNORE_CASE)
private const val STRESS_MARK = "́"
