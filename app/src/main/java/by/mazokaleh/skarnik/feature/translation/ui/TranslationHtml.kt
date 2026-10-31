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

/**
 * Converts a translation's HTML into styled text: colours, bold, italic, underline, sizes, line breaks and
 * links (reported to [onLink] with their `href`). Unknown tags keep their text and drop their styling.
 * Stress table cells use it too.
 */
fun translationHtmlToAnnotatedString(
    html: String,
    colors: TranslationColors,
    onLink: (String) -> Unit
): AnnotatedString = HtmlTextBuilder(colors, onLink).build(html)

private class HtmlTextBuilder(private val colors: TranslationColors, private val onLink: (String) -> Unit) {
    private val builder = AnnotatedString.Builder()
    private var lastChar: Char? = null

    fun build(html: String): AnnotatedString {
        Jsoup.parseBodyFragment(html).body().childNodes().forEach(::appendNode)
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
    }

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
        fontScaleOf(element.attr("size"))?.let { style = (style ?: SpanStyle()).copy(fontSize = it.em) }
        return style
    }

    private fun colorOf(element: Element): Color? {
        val raw = element.attr("color").ifBlank {
            CSS_COLOR.find(element.attr("style"))?.groupValues?.get(1).orEmpty()
        }
        if (raw.isBlank()) return null
        return colors.of(HtmlColorRole.of(raw) ?: HtmlColorRole.Plain)
    }

    /** `<font size>`: relative (`+2`) or absolute (1–7, where 3 is normal), scaled gently around body text. */
    private fun fontScaleOf(size: String): Float? {
        val trimmed = size.trim()
        val number = trimmed.toIntOrNull() ?: return null
        val step = if (trimmed.startsWith("+") || trimmed.startsWith("-")) number else number - NORMAL_FONT_SIZE
        return FONT_SCALES[step.coerceIn(-2, 2) + 2].takeIf { it != 1f }
    }

    /** HTML whitespace: runs collapse to one space, and spaces at the start of a line are dropped. */
    private fun appendText(text: String) {
        val collapsed = WHITESPACE.replace(text, " ")
        if (collapsed.isEmpty()) return
        val dropLeadingSpace = lastChar == null || lastChar == '\n' || lastChar == ' '
        append(if (dropLeadingSpace) collapsed.trimStart(' ') else collapsed)
    }

    private fun startLine() {
        if (lastChar != null && lastChar != '\n') append("\n")
    }

    private fun append(text: String) {
        if (text.isEmpty()) return
        builder.append(text)
        lastChar = text.last()
    }

    companion object {
        private val SKIPPED_TAGS = setOf("script", "style", "head", "title")
        private val BLOCK_TAGS = setOf("p", "div", "li", "ul", "ol", "table", "tr", "h1", "h2", "h3", "h4")

        // Only ASCII whitespace collapses; &nbsp; (U+00A0) is kept, as the source uses it for indents.
        private val WHITESPACE = Regex("[ \\t\\n\\r\\f]+")
        private val CSS_COLOR = Regex("""(?i)(?:^|;)\s*color\s*:\s*(#?[0-9a-f]{3,6})""")
        private const val NORMAL_FONT_SIZE = 3
        private const val SMALL_SIZE = 0.75f
        private val FONT_SCALES = floatArrayOf(0.85f, 0.92f, 1f, 1.12f, 1.25f)
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
