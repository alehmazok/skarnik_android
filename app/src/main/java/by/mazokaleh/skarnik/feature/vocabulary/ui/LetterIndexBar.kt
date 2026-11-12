package by.mazokaleh.skarnik.feature.vocabulary.ui

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.systemGestureExclusion
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.hideFromAccessibility
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.setProgress
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import by.mazokaleh.skarnik.R
import kotlin.math.roundToInt

/**
 * A vertical letter index: touching or dragging along it calls [onLetter] with the index of the letter under
 * the finger, once per letter; [onRelease] when the finger lifts. The bar sits at the screen edge, so it asks
 * the system not to treat drags on it as the back gesture.
 *
 * For TalkBack the bar is one adjustable node (swipe up or down moves a letter) with a «Перайсці да літары»
 * action per letter. The letters themselves are plain text: a clickable letter would be far below the 48dp
 * touch target, which Play's pre-launch accessibility check reports.
 */
@Composable
fun LetterIndexBar(
    letters: List<String>,
    onLetter: (Int) -> Unit,
    onRelease: () -> Unit,
    modifier: Modifier = Modifier
) {
    val haptics = LocalHapticFeedback.current
    val currentOnLetter by rememberUpdatedState(onLetter)
    val currentOnRelease by rememberUpdatedState(onRelease)
    val currentLetters by rememberUpdatedState(letters)
    val tracker = remember { LetterTracker() }
    // The letter TalkBack announces as the bar's state: the last one jumped to, by touch or by TalkBack.
    var selected by remember(letters) { mutableIntStateOf(0) }
    val jump = { index: Int ->
        selected = index
        currentOnLetter(index)
        currentOnRelease()
    }
    val indexLabel = stringResource(R.string.vocabulary_letter_index)
    val jumpLabels = letters.map { stringResource(R.string.vocabulary_jump_to_letter, it) }

    Column(
        modifier
            .width(28.dp)
            .systemGestureExclusion()
            .padding(vertical = 4.dp)
            .letterIndexSemantics(indexLabel, letters, jumpLabels, selected, jump)
            .pointerInput(Unit) {
                // Keyed on Unit and reading the latest letters: restarting would cancel a drag in progress.
                trackVerticalTouch(
                    onMove = { y ->
                        val index = letterAt(y, size.height, currentLetters.size)
                        if (index != null && tracker.moveTo(index)) {
                            haptics.performHapticFeedback(HapticFeedbackType.SegmentFrequentTick)
                            selected = index
                            currentOnLetter(index)
                        }
                    },
                    onEnd = {
                        tracker.reset()
                        currentOnRelease()
                    }
                )
            }
    ) {
        letters.forEach { letter ->
            Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                // TalkBack reads the bar's state instead of each letter.
                Text(
                    text = letter,
                    modifier = Modifier.semantics { hideFromAccessibility() },
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

/**
 * The bar as one adjustable TalkBack control: [letters] at [selected] as its state, a swipe up or down moves one
 * letter, and [jumpLabels] are actions that go straight to a letter. Each calls [jump] with the letter's index.
 */
private fun Modifier.letterIndexSemantics(
    label: String,
    letters: List<String>,
    jumpLabels: List<String>,
    selected: Int,
    jump: (Int) -> Unit
): Modifier = semantics {
    contentDescription = label
    stateDescription = letters.getOrElse(selected) { "" }
    if (letters.size > 1) {
        progressBarRangeInfo = ProgressBarRangeInfo(
            current = selected.toFloat(),
            range = 0f..letters.lastIndex.toFloat(),
            steps = letterSteps(letters.size)
        )
        setProgress { value ->
            jump(letterForProgress(value, letters.size))
            true
        }
    }
    customActions = jumpLabels.mapIndexed { index, actionLabel ->
        CustomAccessibilityAction(actionLabel) {
            jump(index)
            true
        }
    }
}

/** The discrete steps between the first and the last of [count] letters, so one TalkBack swipe moves one letter. */
internal fun letterSteps(count: Int): Int = (count - 2).coerceAtLeast(0)

/** The letter a TalkBack adjustment to [value] lands on, rounded and clamped to the [count] letters. */
internal fun letterForProgress(value: Float, count: Int): Int = value.roundToInt().coerceIn(0, count - 1)

/** The index of the letter at [y] on a bar [height] pixels tall, clamped to the bar; null when it is empty. */
internal fun letterAt(y: Float, height: Int, count: Int): Int? =
    if (count == 0 || height <= 0) null else (y / height * count).toInt().coerceIn(0, count - 1)

/** The letter last reported during one touch, so moving within a letter does not jump again. */
private class LetterTracker {
    private var current = -1

    fun moveTo(index: Int): Boolean = (index != current).also { current = index }

    fun reset() {
        current = -1
    }
}

private suspend fun PointerInputScope.trackVerticalTouch(onMove: (Float) -> Unit, onEnd: () -> Unit) {
    awaitEachGesture {
        val down = awaitFirstDown()
        down.consume()
        onMove(down.position.y)
        while (true) {
            val change = awaitPointerEvent().changes.firstOrNull { it.id == down.id }
            if (change == null || !change.pressed) break
            change.consume()
            onMove(change.position.y)
        }
        onEnd()
    }
}
