package by.mazokaleh.skarnik.feature.settings.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import by.mazokaleh.skarnik.R
import by.mazokaleh.skarnik.core.model.Dictionary
import by.mazokaleh.skarnik.feature.offline.domain.DownloadProgress
import by.mazokaleh.skarnik.feature.offline.domain.OfflineDictionaryStatus
import by.mazokaleh.skarnik.ui.theme.SkarnikTheme
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable

@Serializable
data object SettingsRoute

object SettingsTestTags {
    const val BACK = "settings_back"
    const val OFFLINE_HEADER = "settings_offline_header"
    const val OFFLINE_ERROR = "settings_offline_error"
}

@Composable
fun SettingsScreen(onBack: () -> Unit, viewModel: SettingsViewModel = hiltViewModel()) {
    val offline by viewModel.offline.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val resources = LocalResources.current
    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            val message = when (event) {
                SettingsEvent.RateLimited -> R.string.offline_rate_limited
                SettingsEvent.Deleted -> R.string.offline_deleted
                SettingsEvent.DeleteFailed -> R.string.offline_delete_error
            }
            // A new message replaces the one showing; showSnackbar suspends until dismissed, hence launch.
            snackbarHostState.currentSnackbarData?.dismiss()
            launch { snackbarHostState.showSnackbar(resources.getString(message)) }
        }
    }
    SettingsContent(offline, snackbarHostState, onBack, viewModel::download, viewModel::delete)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SettingsContent(
    offline: OfflineSectionState,
    snackbarHostState: SnackbarHostState,
    onBack: () -> Unit,
    onDownload: (Dictionary) -> Unit,
    onDelete: (Dictionary) -> Unit
) {
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag(SettingsTestTags.BACK)) {
                        Icon(painterResource(R.drawable.ic_arrow_back), stringResource(R.string.action_back))
                    }
                },
                scrollBehavior = scrollBehavior
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        LazyColumn(contentPadding = padding) {
            item(key = "offline_header") { SectionHeader(stringResource(R.string.settings_offline_section)) }
            when (offline) {
                OfflineSectionState.Loading -> item(key = "offline_loading") {
                    Box(Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
                is OfflineSectionState.Ready -> items(offline.items, key = { it.dictionary.path }) { item ->
                    OfflineDictionaryRow(
                        item = item,
                        onDownload = { onDownload(item.dictionary) },
                        onDelete = { onDelete(item.dictionary) }
                    )
                }
                OfflineSectionState.Error -> item(key = "offline_error") {
                    Text(
                        text = stringResource(R.string.database_error),
                        modifier = Modifier.padding(16.dp).testTag(SettingsTestTags.OFFLINE_ERROR),
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}

/** An M3 list subheader. */
@Composable
private fun SectionHeader(text: String) {
    Text(
        text = text,
        modifier = Modifier
            .padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 8.dp)
            .testTag(SettingsTestTags.OFFLINE_HEADER),
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary
    )
}

@Preview
@Composable
private fun SettingsContentPreview() {
    val items = listOf(
        OfflineDictionaryItem(Dictionary.BelRus, OfflineDictionaryStatus.Downloaded(103_412)),
        OfflineDictionaryItem(
            Dictionary.RusBel,
            OfflineDictionaryStatus.Downloading(DownloadProgress(done = 25_000, total = 98_000))
        ),
        OfflineDictionaryItem(Dictionary.Tsbm, OfflineDictionaryStatus.NotDownloaded)
    )
    SkarnikTheme {
        SettingsContent(OfflineSectionState.Ready(items), remember { SnackbarHostState() }, {}, {}, {})
    }
}
