package by.mazokaleh.skarnik.feature.widget.domain

import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import org.jsoup.nodes.Node
import org.jsoup.nodes.TextNode

/**
 * Ported from Flutter's `widget/domain/util/word_similarity.dart` and `html_to_plain_text.dart`: the widget
 * skips words whose translation is basically the same word (Бел-Рус cognates such as «вада» → «вода»).
 */
object WordSimilarity {
    private val BLOCK_TAGS = setOf("p", "div", "br", "li")
    private val APOSTROPHES = listOf("❛", "❜", "`", "‛", "‘", "’")
    private val DASHES = charArrayOf('­', '–', '‑', '—', '‒')
    private const val SIMILARITY_THRESHOLD = 1.8

    /** Translation HTML as plain text; `p`/`div`/`br`/`li` boundaries become line breaks, blank lines go. */
    fun htmlToPlainText(html: String): String {
        val buffer = StringBuilder()
        walk(Jsoup.parseBodyFragment(html).body(), buffer)
        return buffer.lines().map { it.trim() }.filter { it.isNotEmpty() }.joinToString("\n")
    }

    private fun walk(node: Node, buffer: StringBuilder) {
        for (child in node.childNodes()) {
            when (child) {
                is TextNode -> buffer.append(child.wholeText)
                is Element -> {
                    val block = child.normalName() in BLOCK_TAGS
                    if (block) buffer.append('\n')
                    walk(child, buffer)
                    if (block) buffer.append('\n')
                }
            }
        }
    }

    /**
     * Lowercase, trim, then collapse Belarusian/Russian letter differences. The order matters: later
     * replacements act on the output of earlier ones.
     */
    fun normalize(input: String): String {
        var result = input.lowercase().trim()
        result = result.replace("о", "а")
        result = result.replace("щ", "шч")
        result = result.replace("ъ", "'")
        result = result.replace("ў", "у")
        APOSTROPHES.forEach { result = result.replace(it, "'") }
        result = result.replace("ся", "ца")
        result = result.replace("ый", "і")
        result = result.replace("ы", "і")
        result = result.replace("ий", "і")
        result = result.replace("и", "і")
        result = result.replace("т", "ц")
        result = result.replace("ё", "е")
        return result
    }

    fun levenshtein(a: String, b: String): Int {
        if (a == b) return 0
        if (a.isEmpty()) return b.length
        if (b.isEmpty()) return a.length
        var previous = IntArray(b.length + 1) { it }
        var current = IntArray(b.length + 1)
        for (i in 1..a.length) {
            current[0] = i
            for (j in 1..b.length) {
                val cost = if (a[i - 1] == b[j - 1]) 0 else 1
                current[j] = minOf(current[j - 1] + 1, previous[j] + 1, previous[j - 1] + cost)
            }
            val swap = previous
            previous = current
            current = swap
        }
        return previous[b.length]
    }

    /**
     * `word.length / minDistance > 1.8` over the first sense of each translation line (the text before the
     * first dash); distance 0 is always similar.
     */
    fun isSimilar(word: String, translationHtml: String): Boolean {
        val senses = firstSensePerLine(htmlToPlainText(translationHtml))
        if (senses.isEmpty()) return false
        val normalizedWord = normalize(word)
        val minDistance = senses.minOf { levenshtein(normalizedWord, normalize(it)) }
        if (minDistance == 0) return true
        return normalizedWord.length.toDouble() / minDistance > SIMILARITY_THRESHOLD
    }

    private fun firstSensePerLine(plainText: String): List<String> = plainText.lines()
        .map { it.trim() }
        .filter { it.isNotEmpty() }
        .map { line ->
            val cut = line.indexOfAny(DASHES).takeIf { it >= 0 } ?: line.length
            line.substring(0, cut).trim()
        }
        .filter { it.isNotEmpty() }
}
