package by.mazokaleh.skarnik.feature.translation.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.LinkInteractionListener
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.BaselineShift
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.em
import by.mazokaleh.skarnik.feature.translation.domain.AbbreviationScheme
import by.mazokaleh.skarnik.feature.translation.domain.TextPlace
import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import org.jsoup.nodes.Node
import org.jsoup.nodes.TextNode

/**
 * Colours the legacy translation HTML uses, mapped to theme roles (Flutter's `TranslationHtml.colorReplacements`
 * mapped them to fixed iOS colours). Any other colour is drawn as plain text, so it stays readable in every scheme.
 */
enum class HtmlColorRole(vararg val hexes: String) {
    /** Belarusian/Russian headwords and their forms. */
    Accent("831b03", "0000a0", "4863a0"),

    /** Grammar labels: «муж.», «смотрите», parts of speech. */
    Label("008000", "a52a2a", "cc33ff"),

    Plain("000000"),

    Muted("5f5f5f", "151b54"),

    /** Spacers drawn in the page background colour on skarnik.by; Flutter kept them invisible too. */
    Hidden("ffffff");

    companion object {
        private val byHex = entries.flatMap { role -> role.hexes.map { it to role } }.toMap()

        fun of(color: String): HtmlColorRole? = byHex[color.trim().removePrefix("#").lowercase()]
    }
}

data class TranslationColors(
    val accent: Color,
    val label: Color,
    val plain: Color,
    val muted: Color,
    val hidden: Color,
    val link: Color
) {
    fun of(role: HtmlColorRole): Color = when (role) {
        HtmlColorRole.Accent -> accent
        HtmlColorRole.Label -> label
        HtmlColorRole.Plain -> plain
        HtmlColorRole.Muted -> muted
        HtmlColorRole.Hidden -> hidden
    }
}

/** Translation colours from the current colour scheme (ADR 0003). */
@Composable
fun rememberTranslationColors(): TranslationColors {
    val scheme = MaterialTheme.colorScheme
    return remember(scheme) {
        TranslationColors(
            accent = scheme.primary,
            label = scheme.tertiary,
            plain = scheme.onSurface,
            muted = scheme.onSurfaceVariant,
            hidden = scheme.background,
            link = scheme.primary
        )
    }
}

/** An abbreviation in the rendered text, from [start] to [end] (exclusive), with its full text. */
data class AbbreviationSpan(val start: Int, val end: Int, val abbreviation: String, val expansion: String)

/** String annotation on every abbreviation; its item is the expansion. */
const val ABBREVIATION_ANNOTATION = "abbreviation"

/**
 * Converts a translation's HTML into styled text: colours, bold, italic, underline, line breaks and links
 * (reported to [onLink] with their `href`). Unknown tags keep their text and drop their styling. `<font size>` is
 * ignored, so headwords (`<font size="+2">`) match the body text, as in Flutter, which forced one size on every tag.
 * Stress table cells use it too.
 *
 * With [abbreviations], the abbreviations it finds in each kind of text ([TextPlace]: labels, plain text,
 * citations and their sources; never in headwords or links) carry an [ABBREVIATION_ANNOTATION] with their full
 * text (see [abbreviationSpans]).
 */
fun translationHtmlToAnnotatedString(
    html: String,
    colors: TranslationColors,
    abbreviations: AbbreviationScheme? = null,
    onLink: (String) -> Unit
): AnnotatedString = HtmlTextBuilder(colors, abbreviations, onLink).build(html)

private class HtmlTextBuilder(
    private val colors: TranslationColors,
    private val abbreviations: AbbreviationScheme?,
    private val onLink: (String) -> Unit
) {
    private val builder = AnnotatedString.Builder()
    private val plain = StringBuilder()
    private var lastChar: Char? = null

    /** Runs of [plain] to look for abbreviations in, with their place; a source span is always a run of its own. */
    private val runs = mutableListOf<Pair<IntRange, TextPlace>>()

    /** The place of the innermost coloured element, or null inside a link, a headword or hidden text. */
    private val places = ArrayDeque<TextPlace?>()

    /** True while the current line holds nothing or only non-breaking spaces (an indent). */
    private var atLineStart = true

    fun build(html: String): AnnotatedString {
        Jsoup.parseBodyFragment(html).body().childNodes().forEach(::appendNode)
        abbreviations?.let { builder.addAbbreviations(plain, runs, it) }
        val text = builder.toAnnotatedString()
        val end = text.text.indexOfLast { !it.isWhitespace() } + 1
        return if (end == text.length) text else text.subSequence(0, end)
    }

    private fun appendNode(node: Node) {
        when (node) {
            // getWholeText, not text(): jsoup's text() turns &nbsp; into a plain space.
            is TextNode -> appendText(node.wholeText)
            is Element -> appendElement(node)
        }
    }

    private fun appendElement(element: Element) {
        val tag = element.normalName()
        when {
            tag in SKIPPED_TAGS -> Unit
            tag == "br" -> append("\n")
            tag in BLOCK_TAGS -> {
                startLine()
                appendStyled(element)
                startLine()
            }
            else -> appendStyled(element)
        }
    }

    private fun appendStyled(element: Element) {
        val href = element.takeIf { it.normalName() == "a" }?.attr("href")?.takeIf { it.isNotBlank() }
        val style = spanStyleOf(element)
        val change = if (href != null) PlaceChange.To(null) else placeChangeOf(rawColorOf(element))
        val start = plain.length
        // Inside a link or a headword nothing is looked at, whatever the colours inside it.
        if (change is PlaceChange.To) places.addLast(change.place.takeIf { currentPlace != null })
        if (href != null) {
            val link = LinkAnnotation.Clickable(
                tag = href,
                styles = TextLinkStyles(SpanStyle(color = colors.link, textDecoration = TextDecoration.Underline)),
                linkInteractionListener = LinkInteractionListener { onLink(href) }
            )
            builder.pushLink(link)
        }
        if (style != null) builder.pushStyle(style)
        element.childNodes().forEach(::appendNode)
        if (style != null) builder.pop()
        if (href != null) builder.pop()
        if (change is PlaceChange.To) {
            places.removeLast()
            if (change.place == TextPlace.Source && currentPlace != null && plain.length > start) {
                runs += (start until plain.length) to TextPlace.Source
            }
        }
    }

    /** [TextPlace.Plain] outside any coloured element; null inside a link, headword or hidden text. */
    private val currentPlace: TextPlace?
        get() = if (places.isEmpty()) TextPlace.Plain else places.last()

    private fun spanStyleOf(element: Element): SpanStyle? {
        var style = when (element.normalName()) {
            "b", "strong" -> SpanStyle(fontWeight = FontWeight.Bold)
            "i", "em" -> SpanStyle(fontStyle = FontStyle.Italic)
            "u" -> SpanStyle(textDecoration = TextDecoration.Underline)
            "sup" -> SpanStyle(baselineShift = BaselineShift.Superscript, fontSize = SMALL_SIZE.em)
            "sub" -> SpanStyle(baselineShift = BaselineShift.Subscript, fontSize = SMALL_SIZE.em)
            else -> null
        }
        classStyleOf(element, colors)?.let { style = (style ?: SpanStyle()).merge(it) }
        colorOf(element)?.let { style = (style ?: SpanStyle()).copy(color = it) }
        return style
    }

    private fun colorOf(element: Element): Color? {
        val raw = rawColorOf(element)
        if (raw.isBlank()) return null
        return colors.of(HtmlColorRole.of(raw) ?: HtmlColorRole.Plain)
    }

    /**
     * HTML whitespace. At the start of a line only `&nbsp;` is kept: the source indents with it. Inside a line any
     * run of spaces and `&nbsp;` (also across tags) becomes one gap: a lone `&nbsp;` stays non-breaking, a longer
     * run becomes one space, so «— &nbsp;слова» does not leave a wide hole.
     */
    private fun appendText(text: String) {
        TOKEN.findAll(text).forEach { match ->
            val token = match.value
            when {
                token.first() !in SPACES -> append(token)
                atLineStart -> append(token.filter { it == NBSP })
                lastChar == ' ' || lastChar == NBSP -> Unit
                token == NBSP.toString() -> append(token)
                else -> append(" ")
            }
        }
    }

    /** Extends the last run when it has the same place and ends where [range] starts. */
    private fun addRun(range: IntRange) {
        val place = currentPlace?.takeIf { abbreviations != null && it != TextPlace.Source } ?: return
        val last = runs.lastOrNull()
        if (last != null && last.second == place && last.first.last + 1 == range.first) {
            runs[runs.lastIndex] = (last.first.first..range.last) to place
        } else {
            runs += range to place
        }
    }

    private fun startLine() {
        if (lastChar != null && lastChar != '\n') append("\n")
    }

    private fun append(text: String) {
        if (text.isEmpty()) return
        addRun(plain.length until plain.length + text.length)
        builder.append(text)
        plain.append(text)
        lastChar = text.last()
        text.forEach { char ->
            if (char == '\n') {
                atLineStart = true
            } else if (char != NBSP) {
                atLineStart = false
            }
        }
    }

    companion object {
        private val SKIPPED_TAGS = setOf("script", "style", "head", "title")
        private val BLOCK_TAGS = setOf("p", "div", "li", "ul", "ol", "table", "tr", "h1", "h2", "h3", "h4")

        private const val NBSP = '\u00a0'
        private const val SPACES = " \t\n\r\u000c\u00a0"

        /** Runs of HTML whitespace (with `&nbsp;`) and runs of everything else. */
        private val TOKEN = Regex("[ \\t\\n\\r\\f\u00a0]+|[^ \\t\\n\\r\\f\u00a0]+")
        private const val SMALL_SIZE = 0.75f
    }
}

/**
 * Classes of the stress tables (starnik.by and skarnik_admin), styled as in Flutter's `StressHtmlCell`:
 * `pytanne` (the case question) muted and smaller, `skarot` (abbreviations such as «мн.», «я») as a label.
 */
private fun classStyleOf(element: Element, colors: TranslationColors): SpanStyle? = when {
    element.hasClass("pytanne") ->
        SpanStyle(fontStyle = FontStyle.Italic, color = colors.muted, fontSize = QUESTION_SIZE.em)
    element.hasClass("skarot") ->
        SpanStyle(fontStyle = FontStyle.Italic, fontWeight = FontWeight.Normal, color = colors.label)
    else -> null
}

private const val QUESTION_SIZE = 0.875f

private fun rawColorOf(element: Element): String = element.attr("color").ifBlank {
    CSS_COLOR.find(element.attr("style"))?.groupValues?.get(1).orEmpty()
}

/** How an element changes the [TextPlace] of its text: not at all (no colour), or to a place (null: none). */
private sealed interface PlaceChange {
    data object Keep : PlaceChange

    data class To(val place: TextPlace?) : PlaceChange
}

private fun placeChangeOf(rawColor: String): PlaceChange = when (rawColor.trim().removePrefix("#").lowercase()) {
    "" -> PlaceChange.Keep
    SOURCE_COLOR -> PlaceChange.To(TextPlace.Source)
    QUOTE_COLOR -> PlaceChange.To(TextPlace.Quote)
    else -> when (HtmlColorRole.of(rawColor)) {
        HtmlColorRole.Label -> PlaceChange.To(TextPlace.Label)
        HtmlColorRole.Plain, null -> PlaceChange.To(TextPlace.Plain)
        HtmlColorRole.Accent, HtmlColorRole.Hidden, HtmlColorRole.Muted -> PlaceChange.To(null)
    }
}

/** The Тлумачальны слоўнік's citation sources («Колас.») and usage examples and citations. */
private const val SOURCE_COLOR = "151b54"
private const val QUOTE_COLOR = "5f5f5f"

/** Annotates the abbreviations [scheme] finds in the [runs] of [plain] (the builder's text) with their full text. */
private fun AnnotatedString.Builder.addAbbreviations(
    plain: CharSequence,
    runs: List<Pair<IntRange, TextPlace>>,
    scheme: AbbreviationScheme
) {
    runs.forEach { (run, place) ->
        scheme.findIn(plain.substring(run.first, run.last + 1), place).forEach { match ->
            val start = run.first + match.range.first
            addStringAnnotation(ABBREVIATION_ANNOTATION, match.expansion, start, run.first + match.range.last + 1)
        }
    }
}

/** The abbreviations marked by [translationHtmlToAnnotatedString], in text order. */
fun AnnotatedString.abbreviationSpans(): List<AbbreviationSpan> =
    getStringAnnotations(ABBREVIATION_ANNOTATION, 0, length).map {
        AbbreviationSpan(it.start, it.end, text.substring(it.start, it.end), it.item)
    }

private val CSS_COLOR = Regex("""(?i)(?:^|;)\s*color\s*:\s*(#?[0-9a-f]{3,6})""")
