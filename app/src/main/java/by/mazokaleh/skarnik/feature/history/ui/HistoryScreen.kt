package by.mazokaleh.skarnik.feature.history.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import by.mazokaleh.skarnik.R
import by.mazokaleh.skarnik.core.model.Dictionary
import by.mazokaleh.skarnik.core.model.SavedWord
import by.mazokaleh.skarnik.core.ui.ListMessage
import by.mazokaleh.skarnik.core.ui.SavedWordItem
import by.mazokaleh.skarnik.core.ui.SavedWordList
import by.mazokaleh.skarnik.feature.settings.ui.SettingsButton
import by.mazokaleh.skarnik.ui.theme.SkarnikTheme

object HistoryTestTags {
    const val SEARCH = "home_search"
    const val LIST = "history_list"
    const val ITEM = "history_item"
    const val EMPTY = "history_empty"
    const val ERROR = "history_error"
}

/** The start screen: a search bar over the words opened before. */
@Composable
fun HistoryScreen(
    onSearchClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onWordClick: (SavedWord) -> Unit,
    viewModel: HistoryViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    HistoryContent(uiState, onSearchClick, onWordClick, viewModel::loadMore) { SettingsButton(onSettingsClick) }
}

@Composable
private fun HistoryContent(
    uiState: HistoryUiState,
    onSearchClick: () -> Unit,
    onWordClick: (SavedWord) -> Unit,
    onLoadMore: () -> Unit,
    settingsButton: @Composable () -> Unit
) {
    Scaffold(topBar = { SearchEntry(onSearchClick, settingsButton) }) { padding ->
        val modifier = Modifier.padding(padding)
        when (uiState) {
            HistoryUiState.Loading -> Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            is HistoryUiState.Ready -> if (uiState.words.isEmpty()) {
                ListMessage(stringResource(R.string.history_empty), HistoryTestTags.EMPTY, modifier)
            } else {
                SavedWordList(
                    words = uiState.words,
                    hasMore = uiState.hasMore,
                    onLoadMore = onLoadMore,
                    modifier = modifier.testTag(HistoryTestTags.LIST)
                ) { word ->
                    SavedWordItem(
                        word = word,
                        onClick = { onWordClick(word) },
                        modifier = Modifier.animateItem().testTag(HistoryTestTags.ITEM)
                    )
                }
            }
            HistoryUiState.Error -> ListMessage(
                text = stringResource(R.string.database_error),
                testTag = HistoryTestTags.ERROR,
                modifier = modifier,
                isError = true
            )
        }
    }
}

/**
 * A collapsed M3 search bar. It only opens the search screen, so it is a button styled as the bar rather
 * than a focusable text field (which would keep focus and reopen search on return). Settings sit in its
 * trailing slot, as M3 places app-level actions on a search bar.
 */
@Composable
private fun SearchEntry(onClick: () -> Unit, trailing: @Composable () -> Unit) {
    Surface(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.statusBars)
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .height(56.dp)
            .testTag(HistoryTestTags.SEARCH),
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surfaceContainerHigh
    ) {
        Row(Modifier.padding(start = 16.dp, end = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(
                painter = painterResource(R.drawable.ic_search),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = stringResource(R.string.search_placeholder),
                modifier = Modifier.padding(start = 16.dp).weight(1f),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            trailing()
        }
    }
}

@Preview
@Composable
private fun HistoryContentPreview() {
    val words = listOf(
        SavedWord(2, Dictionary.BelRus, 10, "прывітанне", "п"),
        SavedWord(1, Dictionary.Tsbm, 11, "дзякуй", "д")
    )
    SkarnikTheme { HistoryContent(HistoryUiState.Ready(words, hasMore = false), {}, {}, {}, {}) }
}
