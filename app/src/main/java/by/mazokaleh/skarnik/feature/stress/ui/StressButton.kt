package by.mazokaleh.skarnik.feature.stress.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
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
import by.mazokaleh.skarnik.R

object StressButtonTestTags {
    const val BUTTON = "translation_stress"
}

/**
 * Flutter's `ActionStress`: opens the stress table of the one candidate, or a menu of them when the entry has
 * several. M3: an assist chip under the headword instead of a text button in the top app bar.
 */
@Composable
fun StressButton(candidates: List<String>, onSelect: (String) -> Unit, modifier: Modifier = Modifier) {
    if (candidates.isEmpty()) return
    var expanded by rememberSaveable { mutableStateOf(false) }
    Box(modifier) {
        AssistChip(
            onClick = { if (candidates.size == 1) onSelect(candidates.first()) else expanded = true },
            label = { Text(stringResource(R.string.stress_title)) },
            modifier = Modifier.testTag(StressButtonTestTags.BUTTON),
            leadingIcon = {
                Icon(
                    painter = painterResource(R.drawable.ic_stress),
                    contentDescription = null,
                    modifier = Modifier.size(AssistChipDefaults.IconSize),
                    tint = MaterialTheme.colorScheme.primary
                )
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
