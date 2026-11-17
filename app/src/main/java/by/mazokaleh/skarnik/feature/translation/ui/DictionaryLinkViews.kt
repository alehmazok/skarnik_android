package by.mazokaleh.skarnik.feature.translation.ui

import androidx.compose.foundation.layout.size
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import by.mazokaleh.skarnik.R
import by.mazokaleh.skarnik.core.model.Dictionary

object DictionaryLinkTestTags {
    const val CHIP = "translation_dictionary_link_chip"
    const val ROW = "translation_dictionary_link_row"
}

/**
 * Variant `header` of the `dictionary_link_placement` experiment: an assist chip beside «На́ціск», named after what
 * it opens (ТСБМ: «Тлумачэнне», Бел-Рус: «Пераклад»). M3 assist chips are for contextual actions on the content.
 */
@Composable
internal fun DictionaryLinkChip(target: Dictionary, onClick: () -> Unit, modifier: Modifier = Modifier) {
    AssistChip(
        onClick = onClick,
        label = { Text(stringResource(target.chipLabelRes)) },
        modifier = modifier.testTag(DictionaryLinkTestTags.CHIP),
        leadingIcon = {
            Icon(
                painter = painterResource(R.drawable.ic_book_outlined),
                contentDescription = null,
                modifier = Modifier.size(AssistChipDefaults.IconSize)
            )
        }
    )
}

/**
 * Variant `footer`: a card after the translation, where the reader finishes the entry. One tap target, read by
 * TalkBack as its title and text.
 */
@Composable
internal fun DictionaryLinkRow(target: Dictionary, onClick: () -> Unit, modifier: Modifier = Modifier) {
    OutlinedCard(onClick = onClick, modifier = modifier.testTag(DictionaryLinkTestTags.ROW)) {
        ListItem(
            headlineContent = { Text(stringResource(target.rowTitleRes)) },
            supportingContent = { Text(stringResource(target.rowTextRes)) },
            leadingContent = {
                Icon(
                    painter = painterResource(R.drawable.ic_book_outlined),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            },
            trailingContent = {
                Icon(painter = painterResource(R.drawable.ic_chevron_right), contentDescription = null)
            }
        )
    }
}

private val Dictionary.chipLabelRes: Int
    get() = if (this == Dictionary.Tsbm) R.string.dictionary_link_tsbm_chip else R.string.dictionary_link_bel_rus_chip

private val Dictionary.rowTitleRes: Int
    get() = if (this == Dictionary.Tsbm) R.string.dictionary_link_tsbm_title else R.string.dictionary_link_bel_rus_title

private val Dictionary.rowTextRes: Int
    get() = if (this == Dictionary.Tsbm) R.string.dictionary_link_tsbm_text else R.string.dictionary_link_bel_rus_text
