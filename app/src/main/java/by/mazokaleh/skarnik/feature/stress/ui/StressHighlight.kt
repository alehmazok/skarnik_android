package by.mazokaleh.skarnik.feature.stress.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString

private const val STRESS_MARK = '́'

/**
 * [word] with each stressed vowel (the character before a U+0301 combining acute, together with the mark) in
 * [color]; the rest keeps the text colour. A word without a mark is left plain; the text itself is unchanged.
 */
internal fun highlightStress(word: String, color: Color): AnnotatedString = buildAnnotatedString {
    append(word)
    word.forEachIndexed { index, char ->
        if (char == STRESS_MARK && index > 0) addStyle(SpanStyle(color = color), index - 1, index + 1)
    }
}
