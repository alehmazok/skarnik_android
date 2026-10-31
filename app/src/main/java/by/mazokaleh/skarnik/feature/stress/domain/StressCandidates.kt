package by.mazokaleh.skarnik.feature.stress.domain

import by.mazokaleh.skarnik.core.model.Dictionary
import by.mazokaleh.skarnik.core.model.Word
import org.jsoup.Jsoup

/**
 * The words a translation offers stress tables for (Flutter's `Translation.stressCandidates`): the headword of
 * a ТСБМ entry unless it has several words; otherwise every single word inside the entry's `#831b03` `<font>`
 * runs, split at commas, de-duplicated and sorted. Empty means there is no «На́ціск» button.
 */
fun stressCandidates(word: Word, html: String): List<String> = when (word.dictionary) {
    Dictionary.Tsbm -> if (word.word.contains(' ')) emptyList() else listOf(word.word)
    Dictionary.BelRus, Dictionary.RusBel -> candidatesFromHtml(html)
}

private fun candidatesFromHtml(html: String): List<String> = Jsoup.parseBodyFragment(html).select("font[color]")
    .filter { it.attr("color").trim().removePrefix("#").equals(HEADWORD_COLOR, ignoreCase = true) }
    // text() turns &nbsp; into a space, so a phrase joined by &nbsp; is not offered as one word.
    .flatMap { it.text().split(',') }
    .map { it.trim() }
    .filter { it.isNotEmpty() && !it.contains(' ') }
    .distinct()
    .sorted()

private const val HEADWORD_COLOR = "831b03"
