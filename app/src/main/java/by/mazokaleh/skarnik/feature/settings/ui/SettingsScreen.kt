package by.mazokaleh.skarnik.feature.settings.ui

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
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
import by.mazokaleh.skarnik.core.settings.ThemeMode
import by.mazokaleh.skarnik.feature.offline.domain.DownloadProgress
import by.mazokaleh.skarnik.feature.offline.domain.OfflineDictionaryStatus
import by.mazokaleh.skarnik.ui.theme.SkarnikTheme
import by.mazokaleh.skarnik.ui.theme.isDynamicColorAvailable
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable

@Serializable
data object SettingsRoute

object SettingsTestTags {
    const val BACK = "settings_back"
    const val OFFLINE_HEADER = "settings_offline_header"
    const val OFFLINE_ERROR = "settings_offline_error"
    const val APPEARANCE_HEADER = "settings_appearance_header"
    const val OTHER_HEADER = "settings_other_header"
}

/** What the rows do; one object keeps the composables' parameter lists short. */
data class SettingsActions(
    val onBack: () -> Unit,
    val onDownload: (Dictionary) -> Unit,
    val onDelete: (Dictionary) -> Unit,
    val onThemeMode: (ThemeMode) -> Unit,
    val onDynamicColor: (Boolean) -> Unit,
    val onClearHistory: () -> Unit,
    val onWriteToDevelopers: () -> Unit,
    val onRateApp: () -> Unit
)

@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel(),
    generalViewModel: GeneralSettingsViewModel = hiltViewModel()
) {
    val offline by viewModel.offline.collectAsStateWithLifecycle()
    val general by generalViewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val resources = LocalResources.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val showMessage: (Int) -> Unit = { message ->
        // A new message replaces the one showing; showSnackbar suspends until dismissed, hence launch.
        snackbarHostState.currentSnackbarData?.dismiss()
        scope.launch { snackbarHostState.showSnackbar(resources.getString(message)) }
    }
    LaunchedEffect(viewModel, generalViewModel) {
        merge(viewModel.events, generalViewModel.events).collect { showMessage(it.messageRes) }
    }
    val actions = SettingsActions(
        onBack = onBack,
        onDownload = viewModel::download,
        onDelete = viewModel::delete,
        onThemeMode = generalViewModel::setThemeMode,
        onDynamicColor = generalViewModel::setDynamicColor,
        onClearHistory = generalViewModel::clearHistory,
        onWriteToDevelopers = {
            if (!ExternalActions.mailToDevelopers(context)) showMessage(R.string.settings_no_app)
        },
        onRateApp = { if (!ExternalActions.openStoreListing(context)) showMessage(R.string.settings_no_app) }
    )
    SettingsContent(offline, general, snackbarHostState, actions)
}

@get:StringRes
private val SettingsEvent.messageRes: Int
    get() = when (this) {
        SettingsEvent.RateLimited -> R.string.offline_rate_limited
        SettingsEvent.Deleted -> R.string.offline_deleted
        SettingsEvent.DeleteFailed -> R.string.offline_delete_error
        SettingsEvent.HistoryCleared -> R.string.settings_history_cleared
        SettingsEvent.HistoryClearFailed -> R.string.settings_history_clear_error
    }

/** Which dialog or sheet is open; saved, so it survives rotation. */
private enum class SettingsDialog { None, Theme, ClearHistory, About }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SettingsContent(
    offline: OfflineSectionState,
    general: GeneralSettingsState,
    snackbarHostState: SnackbarHostState,
    actions: SettingsActions
) {
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    var dialog by rememberSaveable { mutableStateOf(SettingsDialog.None) }
    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_title)) },
                navigationIcon = {
                    IconButton(onClick = actions.onBack, modifier = Modifier.testTag(SettingsTestTags.BACK)) {
                        Icon(painterResource(R.drawable.ic_arrow_back), stringResource(R.string.action_back))
                    }
                },
                scrollBehavior = scrollBehavior
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        LazyColumn(contentPadding = padding) {
            offlineSection(offline, actions)
            item(key = "appearance_header") {
                SectionHeader(R.string.settings_appearance_section, SettingsTestTags.APPEARANCE_HEADER)
            }
            item(key = "theme") { ThemeRow(general.appearance.themeMode) { dialog = SettingsDialog.Theme } }
            if (isDynamicColorAvailable) {
                item(key = "dynamic_color") {
                    DynamicColorRow(general.appearance.dynamicColor, actions.onDynamicColor)
                }
            }
            otherSection(general.hasHistory, actions, onDialog = { dialog = it })
        }
    }
    when (dialog) {
        SettingsDialog.None -> Unit
        SettingsDialog.Theme -> ThemeDialog(
            current = general.appearance.themeMode,
            onSelect = {
                dialog = SettingsDialog.None
                actions.onThemeMode(it)
            },
            onDismiss = { dialog = SettingsDialog.None }
        )
        SettingsDialog.ClearHistory -> ClearHistoryDialog(
            onConfirm = {
                dialog = SettingsDialog.None
                actions.onClearHistory()
            },
            onDismiss = { dialog = SettingsDialog.None }
        )
        SettingsDialog.About -> AboutSheet(actions.onWriteToDevelopers) { dialog = SettingsDialog.None }
    }
}

private fun LazyListScope.offlineSection(offline: OfflineSectionState, actions: SettingsActions) {
    item(key = "offline_header") { SectionHeader(R.string.settings_offline_section, SettingsTestTags.OFFLINE_HEADER) }
    when (offline) {
        OfflineSectionState.Loading -> item(key = "offline_loading") {
            Box(Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }
        is OfflineSectionState.Ready -> items(offline.items, key = { it.dictionary.path }) { item ->
            OfflineDictionaryRow(
                item = item,
                onDownload = { actions.onDownload(item.dictionary) },
                onDelete = { actions.onDelete(item.dictionary) }
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

private fun LazyListScope.otherSection(
    hasHistory: Boolean,
    actions: SettingsActions,
    onDialog: (SettingsDialog) -> Unit
) {
    item(key = "other_header") { SectionHeader(R.string.settings_other_section, SettingsTestTags.OTHER_HEADER) }
    item(key = "clear_history") {
        SettingsRow(
            title = R.string.settings_clear_history,
            icon = R.drawable.ic_history,
            tag = GeneralSettingsTestTags.CLEAR_HISTORY,
            onClick = { onDialog(SettingsDialog.ClearHistory) },
            enabled = hasHistory
        )
    }
    item(key = "write") {
        SettingsRow(
            title = R.string.settings_write_to_developers,
            icon = R.drawable.ic_mail,
            tag = GeneralSettingsTestTags.WRITE_TO_DEVELOPERS,
            onClick = actions.onWriteToDevelopers
        )
    }
    item(key = "rate") {
        SettingsRow(
            title = R.string.settings_rate_app,
            icon = R.drawable.ic_star_outline,
            tag = GeneralSettingsTestTags.RATE_APP,
            onClick = actions.onRateApp
        )
    }
    item(key = "about") {
        SettingsRow(
            title = R.string.settings_about,
            icon = R.drawable.ic_info,
            tag = GeneralSettingsTestTags.ABOUT,
            onClick = { onDialog(SettingsDialog.About) }
        )
    }
}

/** An M3 list subheader. */
@Composable
private fun SectionHeader(@StringRes text: Int, tag: String) {
    Text(
        text = stringResource(text),
        modifier = Modifier
            .padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 8.dp)
            .testTag(tag),
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
    val actions = SettingsActions({}, {}, {}, {}, {}, {}, {}, {})
    SkarnikTheme {
        SettingsContent(
            OfflineSectionState.Ready(items),
            GeneralSettingsState(hasHistory = true),
            remember { SnackbarHostState() },
            actions
        )
    }
}
