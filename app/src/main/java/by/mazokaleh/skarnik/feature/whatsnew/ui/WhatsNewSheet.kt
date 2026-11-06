package by.mazokaleh.skarnik.feature.whatsnew.ui

import android.os.Build
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import by.mazokaleh.skarnik.R
import by.mazokaleh.skarnik.feature.whatsnew.data.WhatsNewCatalog
import by.mazokaleh.skarnik.feature.whatsnew.domain.WhatsNewItem
import by.mazokaleh.skarnik.feature.whatsnew.domain.WhatsNewRelease
import by.mazokaleh.skarnik.ui.theme.SkarnikTheme
import kotlinx.coroutines.launch

object WhatsNewTestTags {
    const val SHEET = "whats_new_sheet"
    const val CONTINUE = "whats_new_continue"
    const val HISTORY_SHEET = "whats_new_history_sheet"
    const val ITEM = "whats_new_item"
}

/** Shows the notes once after an update, over whatever screen is open. */
@Composable
fun WhatsNewHost(viewModel: WhatsNewViewModel = hiltViewModel()) {
    val release by viewModel.release.collectAsStateWithLifecycle()
    release?.let { WhatsNewSheet(it, onDismiss = viewModel::dismiss) }
}

/** The sheet after an update: a title, the version, the features and one button. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WhatsNewSheet(release: WhatsNewRelease, onDismiss: () -> Unit) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            Modifier
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
                .padding(bottom = 16.dp)
                .testTag(WhatsNewTestTags.SHEET)
        ) {
            Text(
                text = stringResource(R.string.whats_new_title),
                modifier = Modifier.padding(horizontal = 24.dp).semantics { heading() },
                style = MaterialTheme.typography.headlineSmall
            )
            Text(
                text = stringResource(R.string.whats_new_version, release.version),
                modifier = Modifier.padding(start = 24.dp, end = 24.dp, top = 4.dp, bottom = 8.dp),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            release.visibleItems().forEach { WhatsNewItemRow(it) }
            Button(
                // Animates the sheet away, then removes it.
                onClick = {
                    scope.launch { sheetState.hide() }.invokeOnCompletion {
                        if (!sheetState.isVisible) onDismiss()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 24.dp, end = 24.dp, top = 16.dp)
                    .testTag(WhatsNewTestTags.CONTINUE)
            ) {
                Text(stringResource(R.string.whats_new_continue))
            }
        }
    }
}

/** Settings → «Што новага»: every release with notes, newest first. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WhatsNewHistorySheet(releases: List<WhatsNewRelease>, onDismiss: () -> Unit) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ) {
        Column(
            Modifier
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
                .padding(bottom = 16.dp)
                .testTag(WhatsNewTestTags.HISTORY_SHEET)
        ) {
            Text(
                text = stringResource(R.string.settings_whats_new),
                modifier = Modifier.padding(horizontal = 24.dp).semantics { heading() },
                style = MaterialTheme.typography.titleLarge
            )
            releases.forEach { release ->
                Text(
                    text = stringResource(R.string.whats_new_version, release.version),
                    modifier = Modifier
                        .padding(start = 24.dp, end = 24.dp, top = 16.dp, bottom = 4.dp)
                        .semantics { heading() },
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary
                )
                release.visibleItems().forEach { WhatsNewItemRow(it) }
            }
        }
    }
}

/** An M3 list item with the icon in a tonal circle. */
@Composable
private fun WhatsNewItemRow(item: WhatsNewItem) {
    ListItem(
        headlineContent = { Text(stringResource(item.title)) },
        supportingContent = { Text(stringResource(item.text)) },
        leadingContent = {
            Surface(shape = CircleShape, color = MaterialTheme.colorScheme.secondaryContainer) {
                Box(Modifier.size(40.dp), contentAlignment = Alignment.Center) {
                    Icon(painterResource(item.icon), contentDescription = null, Modifier.size(24.dp))
                }
            }
        },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
        modifier = Modifier.testTag(WhatsNewTestTags.ITEM)
    )
}

private fun WhatsNewRelease.visibleItems(): List<WhatsNewItem> = items.filter { Build.VERSION.SDK_INT >= it.minSdk }

@Preview
@Composable
private fun WhatsNewItemsPreview() {
    SkarnikTheme {
        Surface {
            Column {
                WhatsNewCatalog.releases.first().items.forEach { WhatsNewItemRow(it) }
            }
        }
    }
}
