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
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.isSpecified
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.ui.platform.ViewConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import by.mazokaleh.skarnik.feature.translation.domain.AbbreviationScheme
import kotlin.math.roundToInt

object AbbreviationTestTags {
    const val ABBREVIATION = "translation_abbreviation"
    const val TOOLTIP = "abbreviation_tooltip"
    const val TOOLTIP_TITLE = "abbreviation_tooltip_title"
    const val TOOLTIP_TEXT = "abbreviation_tooltip_text"
}

/**
 * The translation text. The [abbreviations] it finds («разг.», «Колас») get a dotted underline; tapping one shows
 * its full text in an M3 rich tooltip above it, until a tap outside.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TranslationText(
    html: String,
    abbreviations: AbbreviationScheme?,
    onLink: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = rememberTranslationColors()
    val currentOnLink by rememberUpdatedState(onLink)
    var tapped by remember(html) { mutableStateOf<AbbreviationSpan?>(null) }
    val text = remember(html, colors, abbreviations) {
        translationHtmlToAnnotatedString(html, colors, abbreviations) { currentOnLink(it) }
    }
    var layout by remember { mutableStateOf<TextLayoutResult?>(null) }

    Box(modifier) {
        Text(
            text = text,
            modifier = Modifier
                .testTag(TranslationTestTags.TEXT)
                .drawBehind { layout?.let { drawAbbreviationUnderlines(it, text, colors.plain) } },
            style = MaterialTheme.typography.bodyLarge,
            onTextLayout = { layout = it }
        )
        val currentLayout = layout
        if (currentLayout != null) {
            AbbreviationTargets(text, currentLayout) { tapped = it }
        }
        val span = tapped
        if (span != null && currentLayout != null && span.end <= currentLayout.layoutInput.text.length) {
            val anchor = remember(span, currentLayout) { currentLayout.firstLineBounds(span.start, span.end) }
            val state = rememberTooltipState(isPersistent = true)
            val density = LocalDensity.current
            TooltipBox(
                positionProvider = TooltipDefaults.rememberTooltipPositionProvider(TooltipAnchorPosition.Above),
                tooltip = {
                    DisableSelection {
                        RichTooltip(
                            modifier = Modifier.testTag(AbbreviationTestTags.TOOLTIP),
                            title = { Text(span.abbreviation, Modifier.testTag(AbbreviationTestTags.TOOLTIP_TITLE)) },
                            caretShape = TooltipDefaults.caretShape()
                        ) {
                            Text(span.expansion, Modifier.testTag(AbbreviationTestTags.TOOLTIP_TEXT))
                        }
                    }
                },
                state = state,
                // Focusable, so TalkBack moves to the tooltip and a tap outside only closes it.
                focusable = true,
                enableUserInput = false,
                modifier = Modifier.offset { IntOffset(anchor.left.roundToInt(), anchor.top.roundToInt()) }
            ) {
                with(density) { Spacer(Modifier.size(anchor.width.toDp(), anchor.height.toDp())) }
            }
            LaunchedEffect(span) {
                try {
                    state.show()
                } finally {
                    if (tapped == span) tapped = null
                }
            }
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

private const val UNDERLINE_POSITION = 0.6f
