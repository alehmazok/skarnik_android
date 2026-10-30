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
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import by.mazokaleh.skarnik.R

/**
 * A vertical letter index: touching or dragging along it calls [onLetter] with the index of the letter under
 * the finger, once per letter; [onRelease] when the finger lifts. TalkBack users get a click action per letter.
 * The bar sits at the screen edge, so it asks the system not to treat drags on it as the back gesture.
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

    Column(
        modifier
            .width(28.dp)
            .systemGestureExclusion()
            .padding(vertical = 4.dp)
            .pointerInput(Unit) {
                // Keyed on Unit and reading the latest letters: restarting would cancel a drag in progress.
                trackVerticalTouch(
                    onMove = { y ->
                        val index = letterAt(y, size.height, currentLetters.size)
                        if (index != null && tracker.moveTo(index)) {
                            haptics.performHapticFeedback(HapticFeedbackType.SegmentFrequentTick)
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
        letters.forEachIndexed { index, letter ->
            IndexLetter(
                letter = letter,
                onJump = {
                    currentOnLetter(index)
                    currentOnRelease()
                },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun IndexLetter(letter: String, onJump: () -> Unit, modifier: Modifier = Modifier) {
    val jumpLabel = stringResource(R.string.vocabulary_jump_to_letter, letter)
    Box(
        modifier
            .fillMaxWidth()
            .semantics(mergeDescendants = true) {
                onClick(label = jumpLabel) {
                    onJump()
                    true
                }
            },
        contentAlignment = Alignment.Center
    ) {
        Text(text = letter, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
    }
}

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
