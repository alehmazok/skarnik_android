package by.mazokaleh.skarnik.feature.settings.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import by.mazokaleh.skarnik.R
import by.mazokaleh.skarnik.core.ui.shortNameRes
import by.mazokaleh.skarnik.feature.offline.domain.OfflineDictionaryStatus

object OfflineDictionaryTestTags {
    /** Followed by the dictionary path: `offline_dictionary_tsbm`. On the dictionary name. */
    const val NAME_PREFIX = "offline_dictionary_"

    /** Followed by the dictionary path. On the first status line. */
    const val STATUS_PREFIX = "offline_status_"
    const val PROGRESS = "offline_progress"
    const val PROGRESS_TEXT = "offline_progress_text"
    const val DELETE = "offline_delete"
    const val DELETE_CONFIRM = "offline_delete_confirm"
}

/**
 * One dictionary: tap to download (or resume after an error), live progress while downloading, and a delete
 * button once downloaded. Deleting asks first.
 */
@Composable
fun OfflineDictionaryRow(item: OfflineDictionaryItem, onDownload: () -> Unit, onDelete: () -> Unit) {
    val name = stringResource(item.dictionary.shortNameRes)
    val path = item.dictionary.path
    var confirmDelete by rememberSaveable { mutableStateOf(false) }
    val status = item.status
    val canStart = status is OfflineDictionaryStatus.NotDownloaded || status is OfflineDictionaryStatus.Failed

    ListItem(
        headlineContent = { Text(name, Modifier.testTag(OfflineDictionaryTestTags.NAME_PREFIX + path)) },
        // Tags go on the texts: a clickable row merges its children, and Maestro sees no text on the merged node.
        supportingContent = {
            Column { StatusLines(status, Modifier.testTag(OfflineDictionaryTestTags.STATUS_PREFIX + path)) }
        },
        leadingContent = { LeadingIcon(status) },
        trailingContent = if (status is OfflineDictionaryStatus.Downloaded) {
            {
                IconButton(
                    onClick = { confirmDelete = true },
                    modifier = Modifier.testTag(OfflineDictionaryTestTags.DELETE)
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_delete),
                        contentDescription = stringResource(R.string.offline_delete),
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        } else {
            null
        },
        modifier = if (canStart) Modifier.clickable(onClick = onDownload) else Modifier
    )

    if (confirmDelete) {
        DeleteDialog(
            name = name,
            onConfirm = {
                confirmDelete = false
                onDelete()
            },
            onDismiss = { confirmDelete = false }
        )
    }
}

@Composable
private fun StatusLines(status: OfflineDictionaryStatus, statusModifier: Modifier) {
    when (status) {
        OfflineDictionaryStatus.NotDownloaded -> Text(stringResource(R.string.offline_download), statusModifier)
        OfflineDictionaryStatus.Failed -> Text(
            text = stringResource(R.string.offline_download_error),
            modifier = statusModifier,
            color = MaterialTheme.colorScheme.error
        )
        is OfflineDictionaryStatus.Downloading -> {
            Text(stringResource(R.string.offline_downloading), statusModifier)
            val progress = status.progress
            val indicatorModifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp)
                .testTag(OfflineDictionaryTestTags.PROGRESS)
            if (progress == null) {
                LinearProgressIndicator(indicatorModifier)
            } else {
                LinearProgressIndicator(progress = { progress.fraction }, modifier = indicatorModifier)
                val total = progress.total.toInt()
                Text(
                    text = pluralStringResource(R.plurals.offline_progress, total, progress.done.toInt(), total),
                    modifier = Modifier.testTag(OfflineDictionaryTestTags.PROGRESS_TEXT)
                )
            }
        }
        is OfflineDictionaryStatus.Downloaded -> {
            val count = status.wordCount.toInt()
            Text(pluralStringResource(R.plurals.offline_downloaded, count, count), statusModifier)
        }
    }
}

@Composable
private fun LeadingIcon(status: OfflineDictionaryStatus) {
    when (status) {
        is OfflineDictionaryStatus.Downloading -> CircularProgressIndicator(Modifier.size(24.dp), strokeWidth = 2.dp)
        is OfflineDictionaryStatus.Downloaded -> Icon(painterResource(R.drawable.ic_download_done), null)
        else -> Icon(painterResource(R.drawable.ic_download), null)
    }
}

@Composable
private fun DeleteDialog(name: String, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.offline_delete_title)) },
        text = { Text(stringResource(R.string.offline_delete_confirmation, name)) },
        confirmButton = {
            TextButton(onClick = onConfirm, modifier = Modifier.testTag(OfflineDictionaryTestTags.DELETE_CONFIRM)) {
                Text(stringResource(R.string.action_delete))
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) } }
    )
}
