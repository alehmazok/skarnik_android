package by.mazokaleh.skarnik.core.ui

import androidx.compose.material3.ListItemColors
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import by.mazokaleh.skarnik.core.model.Dictionary
import by.mazokaleh.skarnik.core.model.WordLink

/**
 * Colours of a word row. The row whose translation shows beside the list (wide screens) is filled with the
 * secondary container, as M3 marks the selected item of a list–detail layout.
 */
@Composable
fun wordListItemColors(selected: Boolean, containerColor: Color = ListItemDefaults.containerColor): ListItemColors =
    ListItemDefaults.colors(
        containerColor = if (selected) MaterialTheme.colorScheme.secondaryContainer else containerColor
    )

/** TalkBack announces the selected row; other rows stay plain buttons, as on phones. */
fun Modifier.selectedWord(selected: Boolean): Modifier = if (selected) semantics { this.selected = true } else this

/** Whether this link (the word open beside a list, or null) points to the word [wordId] of [dictionary]. */
fun WordLink?.isWord(dictionary: Dictionary, wordId: Long): Boolean =
    this != null && this.dictionary == dictionary && this.wordId == wordId
