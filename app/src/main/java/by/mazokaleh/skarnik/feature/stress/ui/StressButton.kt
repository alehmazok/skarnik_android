package by.mazokaleh.skarnik.feature.stress.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import by.mazokaleh.skarnik.R

object StressButtonTestTags {
    const val BUTTON = "translation_stress"
    const val LOADING = "translation_stress_loading"
}

/**
 * Flutter's `ActionStress`: opens the stress table of the one candidate, or a menu of them when the entry has
 * several. M3: an assist chip under the headword instead of a text button in the top app bar. No leading icon, as
 * in Flutter: the usual candidates (a speaking head, a volume icon) promise audio, and the chip opens a table.
 * TalkBack hears the label without the acute accent, which some voices read out as a separate sign. While the
 * word is looked up ([loading]) the chip shows a spinner and ignores taps.
 */
@Composable
fun StressButton(
    candidates: List<String>,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
    loading: Boolean = false
) {
    if (candidates.isEmpty()) return
    var expanded by rememberSaveable { mutableStateOf(false) }
    val spokenLabel = stringResource(R.string.stress_title_spoken)
    Box(modifier) {
        AssistChip(
            onClick = {
                when {
                    loading -> Unit
                    candidates.size == 1 -> onSelect(candidates.first())
                    else -> expanded = true
                }
            },
            label = { Text(stringResource(R.string.stress_title)) },
            modifier = Modifier
                .testTag(StressButtonTestTags.BUTTON)
                .semantics { contentDescription = spokenLabel },
            leadingIcon = if (loading) {
                {
                    CircularProgressIndicator(
                        modifier = Modifier.size(AssistChipDefaults.IconSize).testTag(StressButtonTestTags.LOADING),
                        strokeWidth = 2.dp
                    )
                }
            } else {
                null
            },
            trailingIcon = if (candidates.size > 1) {
                {
                    Icon(
                        painter = painterResource(R.drawable.ic_arrow_drop_down),
                        contentDescription = null,
                        modifier = Modifier.size(AssistChipDefaults.IconSize)
                    )
                }
            } else {
                null
            }
        )
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            candidates.forEach { candidate ->
                DropdownMenuItem(
                    text = { Text(candidate, style = MaterialTheme.typography.bodyLarge) },
                    onClick = {
                        expanded = false
                        onSelect(candidate)
                    }
                )
            }
        }
    }
}
