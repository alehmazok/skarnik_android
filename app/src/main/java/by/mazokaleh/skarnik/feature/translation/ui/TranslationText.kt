package by.mazokaleh.skarnik.feature.translation.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.selection.DisableSelection
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RichTooltip
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TooltipAnchorPosition
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.isSpecified
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.ui.platform.ViewConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import by.mazokaleh.skarnik.R
import by.mazokaleh.skarnik.core.model.WordLink
import by.mazokaleh.skarnik.feature.translation.domain.AbbreviationScheme
import kotlin.math.roundToInt
import kotlinx.coroutines.launch

object WordLinkTestTags {
    const val WORD_LINK = "translation_word_link"
    const val HINT = "word_link_hint"
    const val HINT_DONE = "word_link_hint_done"
}

object AbbreviationTestTags {
    const val ABBREVIATION = "translation_abbreviation"
    const val TOOLTIP = "abbreviation_tooltip"
    const val TOOLTIP_TITLE = "abbreviation_tooltip_title"
    const val TOOLTIP_TEXT = "abbreviation_tooltip_text"
}

/**
 * The translation text. The [abbreviations] it finds («разг.», «Колас») get a dotted underline; tapping one shows
 * its full text in an M3 rich tooltip above it, until a tap outside.
 *
 * The Belarusian words in [wordLinks] open their entry through [onWordLink]; they look as before (Aleh,
 * 2026-11-16), except while [hintVisible]: then they are underlined and a tooltip on the first one explains them,
 * until [onHintDone] (true when a word was tapped). [onWordLinksShown] reports that the text has such words.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TranslationText(
    html: String,
    abbreviations: AbbreviationScheme?,
    onLink: (String) -> Unit,
    modifier: Modifier = Modifier,
    wordLinks: Map<String, WordLink> = emptyMap(),
    onWordLink: (WordLinkSpan) -> Unit = {},
    hintVisible: Boolean = false,
    onWordLinksShown: () -> Unit = {},
    onHintDone: (byTap: Boolean) -> Unit = {}
) {
    val colors = rememberTranslationColors()
    val currentOnLink by rememberUpdatedState(onLink)
    var tapped by remember(html) { mutableStateOf<AbbreviationSpan?>(null) }
    val text = remember(html, colors, abbreviations, wordLinks) {
        translationHtmlToAnnotatedString(html, colors, abbreviations, wordLinks) { currentOnLink(it) }
    }
    val hasWordLinks = remember(text) { text.wordLinkSpans().isNotEmpty() }
    val currentOnWordLinksShown by rememberUpdatedState(onWordLinksShown)
    LaunchedEffect(hasWordLinks) { if (hasWordLinks) currentOnWordLinksShown() }
    var layout by remember { mutableStateOf<TextLayoutResult?>(null) }

    Box(modifier) {
        Text(
            text = text,
            modifier = Modifier
                .testTag(TranslationTestTags.TEXT)
                .drawBehind {
                    layout?.let {
                        drawAbbreviationUnderlines(it, text, colors.plain)
                        if (hintVisible) drawWordLinkUnderlines(it, text, colors.accent)
                    }
                },
            style = MaterialTheme.typography.bodyLarge,
            onTextLayout = { layout = it }
        )
        val currentLayout = layout
        if (currentLayout != null) {
            AbbreviationTargets(text, currentLayout) { tapped = it }
            WordLinkTargets(text, currentLayout) {
                if (hintVisible) onHintDone(true)
                onWordLink(it)
            }
            if (hintVisible) WordLinkHint(text, currentLayout, onDone = { onHintDone(false) })
        }
        val span = tapped
        if (span != null && currentLayout != null && span.end <= currentLayout.layoutInput.text.length) {
            AbbreviationTooltip(span, currentLayout) { if (tapped == span) tapped = null }
        }
    }
}

/** The full text of the tapped abbreviation, above it, until a tap outside; then [onDone]. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AbbreviationTooltip(span: AbbreviationSpan, layout: TextLayoutResult, onDone: () -> Unit) {
    val anchor = remember(span, layout) { layout.firstLineBounds(span.start, span.end) }
    val state = rememberTooltipState(isPersistent = true)
    val density = LocalDensity.current
    val currentOnDone by rememberUpdatedState(onDone)
    // The offset goes on a box around the TooltipBox: its popup is anchored to its own outer box, which
    // ignores the modifier passed to TooltipBox, so with the offset there it always opened at the top.
    Box(Modifier.offset { IntOffset(anchor.left.roundToInt(), anchor.top.roundToInt()) }) {
        TooltipBox(
            positionProvider = TooltipDefaults.rememberTooltipPositionProvider(TooltipAnchorPosition.Above),
            tooltip = {
                DisableSelection {
                    RichTooltip(
                        modifier = Modifier.testTag(AbbreviationTestTags.TOOLTIP),
                        title = {
                            Text(span.abbreviation, Modifier.testTag(AbbreviationTestTags.TOOLTIP_TITLE))
                        },
                        caretShape = TooltipDefaults.caretShape()
                    ) {
                        Text(span.expansion, Modifier.testTag(AbbreviationTestTags.TOOLTIP_TEXT))
                    }
                }
            },
            state = state,
            // Focusable, so TalkBack moves to the tooltip and a tap outside only closes it.
            focusable = true,
            enableUserInput = false
        ) {
            with(density) { Spacer(Modifier.size(anchor.width.toDp(), anchor.height.toDp())) }
        }
    }
    LaunchedEffect(span) {
        try {
            state.show()
        } finally {
            currentOnDone()
        }
    }
}

/**
 * A tap target over each abbreviation, so a tap anywhere on it opens the tooltip, and TalkBack and Maestro see it as
 * its own element («ср.: средний род»). Without the 48 dp minimum, which would shift neighbouring targets.
 */
@Composable
private fun AbbreviationTargets(text: AnnotatedString, layout: TextLayoutResult, onTap: (AbbreviationSpan) -> Unit) {
    val spans = remember(text, layout) {
        text.abbreviationSpans().filter { it.end <= layout.layoutInput.text.length }
            .map { it to layout.firstLineBounds(it.start, it.end) }
    }
    val density = LocalDensity.current
    val base = LocalViewConfiguration.current
    val noMinimumTarget = remember(base) {
        object : ViewConfiguration by base {
            override val minimumTouchTargetSize: DpSize = DpSize.Zero
        }
    }
    CompositionLocalProvider(LocalViewConfiguration provides noMinimumTarget) {
        spans.forEach { (span, bounds) ->
            Box(
                Modifier
                    .offset { IntOffset(bounds.left.roundToInt(), bounds.top.roundToInt()) }
                    .size(with(density) { bounds.width.toDp() }, with(density) { bounds.height.toDp() })
                    .testTag(AbbreviationTestTags.ABBREVIATION)
                    .semantics { contentDescription = "${span.abbreviation}: ${span.expansion}" }
                    .clickable { onTap(span) }
            )
        }
    }
}

/** A tap target over each linked word, like [AbbreviationTargets]: TalkBack reads the word and «адкрыць пераклад». */
@Composable
private fun WordLinkTargets(text: AnnotatedString, layout: TextLayoutResult, onTap: (WordLinkSpan) -> Unit) {
    val spans = remember(text, layout) {
        text.wordLinkSpans().filter { it.end <= layout.layoutInput.text.length }
            .map { it to layout.firstLineBounds(it.start, it.end) }
    }
    val density = LocalDensity.current
    val base = LocalViewConfiguration.current
    val noMinimumTarget = remember(base) {
        object : ViewConfiguration by base {
            override val minimumTouchTargetSize: DpSize = DpSize.Zero
        }
    }
    val action = stringResource(R.string.translation_word_link_action)
    CompositionLocalProvider(LocalViewConfiguration provides noMinimumTarget) {
        spans.forEach { (span, bounds) ->
            Box(
                Modifier
                    .offset { IntOffset(bounds.left.roundToInt(), bounds.top.roundToInt()) }
                    .size(with(density) { bounds.width.toDp() }, with(density) { bounds.height.toDp() })
                    .testTag(WordLinkTestTags.WORD_LINK)
                    .semantics { contentDescription = span.word }
                    .clickable(onClickLabel = action, role = Role.Button) { onTap(span) }
            )
        }
    }
}

/**
 * The one-time hint: a rich tooltip above the first linked word, closed by its button. Not focusable, so the rest
 * of the screen stays usable while it shows.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WordLinkHint(text: AnnotatedString, layout: TextLayoutResult, onDone: () -> Unit) {
    val first = remember(text, layout) {
        text.wordLinkSpans().firstOrNull { it.end <= layout.layoutInput.text.length }
    } ?: return
    val anchor = remember(first, layout) { layout.firstLineBounds(first.start, first.end) }
    val state = rememberTooltipState(isPersistent = true)
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current
    val currentOnDone by rememberUpdatedState(onDone)
    Box(Modifier.offset { IntOffset(anchor.left.roundToInt(), anchor.top.roundToInt()) }) {
        TooltipBox(
            positionProvider = TooltipDefaults.rememberTooltipPositionProvider(TooltipAnchorPosition.Above),
            tooltip = {
                DisableSelection {
                    RichTooltip(
                        modifier = Modifier.testTag(WordLinkTestTags.HINT),
                        caretShape = TooltipDefaults.caretShape(),
                        action = {
                            TextButton(
                                onClick = { scope.launch { state.dismiss() } },
                                modifier = Modifier.testTag(WordLinkTestTags.HINT_DONE)
                            ) { Text(stringResource(R.string.translation_word_link_hint_done)) }
                        }
                    ) {
                        Text(stringResource(R.string.translation_word_link_hint))
                    }
                }
            },
            state = state,
            focusable = false,
            enableUserInput = false
        ) {
            with(density) { Spacer(Modifier.size(anchor.width.toDp(), anchor.height.toDp())) }
        }
    }
    LaunchedEffect(Unit) {
        try {
            state.show()
        } finally {
            currentOnDone()
        }
    }
}

/** Bounds of [start] until [end] on the line of [start]. */
private fun TextLayoutResult.firstLineBounds(start: Int, end: Int): Rect {
    val line = getLineForOffset(start)
    val lineEnd = minOf(end, getLineEnd(line, visibleEnd = true))
    val left = getHorizontalPosition(start, usePrimaryDirection = true)
    val right = getHorizontalPosition(lineEnd, usePrimaryDirection = true)
    return Rect(minOf(left, right), getLineTop(line), maxOf(left, right), getLineBottom(line))
}

/** Dotted underlines in the colour of each abbreviation's text, else [default]. */
private fun DrawScope.drawAbbreviationUnderlines(layout: TextLayoutResult, text: AnnotatedString, default: Color) {
    val length = layout.layoutInput.text.length
    val stroke = 1.dp.toPx()
    val dash = PathEffect.dashPathEffect(floatArrayOf(2.dp.toPx(), 2.dp.toPx()))
    text.getStringAnnotations(ABBREVIATION_ANNOTATION, 0, text.length).filter { it.end <= length }.forEach { range ->
        // The innermost style with a colour: styles are listed in the order they were opened.
        val color = text.spanStyles.lastOrNull {
            it.start <= range.start && it.end >= range.end && it.item.color.isSpecified
        }?.item?.color ?: default
        for (line in layout.getLineForOffset(range.start)..layout.getLineForOffset(range.end - 1)) {
            val start = maxOf(range.start, layout.getLineStart(line))
            val end = minOf(range.end, layout.getLineEnd(line, visibleEnd = true))
            if (end <= start) continue
            val left = layout.getHorizontalPosition(start, usePrimaryDirection = true)
            val right = layout.getHorizontalPosition(end, usePrimaryDirection = true)
            val baseline = layout.getLineBaseline(line)
            // Below the descenders («р», «д»), inside the line's own height.
            val y = baseline + (layout.getLineBottom(line) - baseline) * UNDERLINE_POSITION
            drawLine(color, Offset(left, y), Offset(right, y), strokeWidth = stroke, pathEffect = dash)
        }
    }
}

/** Solid underlines under the linked words, in [color], while the hint shows. */
private fun DrawScope.drawWordLinkUnderlines(layout: TextLayoutResult, text: AnnotatedString, color: Color) {
    val length = layout.layoutInput.text.length
    val stroke = 1.dp.toPx()
    text.wordLinkSpans().filter { it.end <= length }.forEach { span ->
        for (line in layout.getLineForOffset(span.start)..layout.getLineForOffset(span.end - 1)) {
            val start = maxOf(span.start, layout.getLineStart(line))
            val end = minOf(span.end, layout.getLineEnd(line, visibleEnd = true))
            if (end <= start) continue
            val left = layout.getHorizontalPosition(start, usePrimaryDirection = true)
            val right = layout.getHorizontalPosition(end, usePrimaryDirection = true)
            val baseline = layout.getLineBaseline(line)
            val y = baseline + (layout.getLineBottom(line) - baseline) * UNDERLINE_POSITION
            drawLine(color, Offset(left, y), Offset(right, y), strokeWidth = stroke)
        }
    }
}

private const val UNDERLINE_POSITION = 0.6f
