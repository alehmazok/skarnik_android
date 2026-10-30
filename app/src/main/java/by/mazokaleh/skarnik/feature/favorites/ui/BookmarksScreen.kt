package by.mazokaleh.skarnik.feature.favorites.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberSwipeToDismissBoxState
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
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
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
import by.mazokaleh.skarnik.feature.favorites.domain.BookmarksSortOrder
import by.mazokaleh.skarnik.ui.theme.SkarnikTheme
import kotlinx.coroutines.launch

object BookmarksTestTags {
    const val LIST = "bookmarks_list"
    const val ITEM = "bookmarks_item"
    const val EMPTY = "bookmarks_empty"
    const val ERROR = "bookmarks_error"
    const val SORT = "bookmarks_sort"
    const val SORT_BY_DATE = "bookmarks_sort_date"
    const val SORT_BY_LETTER = "bookmarks_sort_letter"
}

@Composable
fun BookmarksScreen(onWordClick: (SavedWord) -> Unit, viewModel: BookmarksViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val resources = LocalResources.current
    val scope = rememberCoroutineScope()

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                is BookmarksEvent.Removed -> scope.launch {
                    // One snackbar at a time, as in Flutter: an older removal simply stays.
                    snackbarHostState.currentSnackbarData?.dismiss()
                    val result = snackbarHostState.showSnackbar(
                        message = resources.getString(R.string.bookmarks_removed),
                        actionLabel = resources.getString(R.string.action_undo),
                        withDismissAction = false
                    )
                    if (result == SnackbarResult.ActionPerformed) viewModel.undoRemove(event.bookmark)
                }
            }
        }
    }

    BookmarksContent(
        uiState = uiState,
        snackbarHostState = snackbarHostState,
        onWordClick = onWordClick,
        onRemove = viewModel::remove,
        onSortOrderChange = viewModel::setSortOrder,
        onLoadMore = viewModel::loadMore
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BookmarksContent(
    uiState: BookmarksUiState,
    snackbarHostState: SnackbarHostState,
    onWordClick: (SavedWord) -> Unit,
    onRemove: (SavedWord) -> Unit,
    onSortOrderChange: (BookmarksSortOrder) -> Unit,
    onLoadMore: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.bookmarks_title)) },
                actions = {
                    if (uiState is BookmarksUiState.Ready) SortMenu(uiState.sortOrder, onSortOrderChange)
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        val modifier = Modifier.padding(padding)
        when (uiState) {
            BookmarksUiState.Loading -> Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            is BookmarksUiState.Ready -> if (uiState.words.isEmpty()) {
                ListMessage(stringResource(R.string.bookmarks_empty), BookmarksTestTags.EMPTY, modifier)
            } else {
                SavedWordList(
                    words = uiState.words,
                    hasMore = uiState.hasMore,
                    onLoadMore = onLoadMore,
                    modifier = modifier.testTag(BookmarksTestTags.LIST)
                ) { word ->
                    BookmarkItem(word, onWordClick, onRemove, Modifier.animateItem())
                }
            }
            BookmarksUiState.Error -> ListMessage(
                text = stringResource(R.string.database_error),
                testTag = BookmarksTestTags.ERROR,
                modifier = modifier,
                isError = true
            )
        }
    }
}

@Composable
private fun SortMenu(sortOrder: BookmarksSortOrder, onSortOrderChange: (BookmarksSortOrder) -> Unit) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    Box {
        IconButton(onClick = { expanded = true }, modifier = Modifier.testTag(BookmarksTestTags.SORT)) {
            Icon(painterResource(R.drawable.ic_sort), contentDescription = stringResource(R.string.bookmarks_sort))
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            listOf(
                Triple(BookmarksSortOrder.DateAdded, R.string.bookmarks_sort_by_date, BookmarksTestTags.SORT_BY_DATE),
                Triple(
                    BookmarksSortOrder.Alphabetical,
                    R.string.bookmarks_sort_by_letter,
                    BookmarksTestTags.SORT_BY_LETTER
                )
            ).forEach { (order, label, tag) ->
                DropdownMenuItem(
                    text = { Text(stringResource(label)) },
                    onClick = {
                        expanded = false
                        onSortOrderChange(order)
                    },
                    modifier = Modifier.testTag(tag),
                    trailingIcon = {
                        if (order == sortOrder) Icon(painterResource(R.drawable.ic_check), contentDescription = null)
                    }
                )
            }
        }
    }
}

/** Swipe from the end to remove, as in Flutter; TalkBack users get the same as a custom action. */
@Composable
private fun BookmarkItem(
    word: SavedWord,
    onClick: (SavedWord) -> Unit,
    onRemove: (SavedWord) -> Unit,
    modifier: Modifier = Modifier
) {
    val state = rememberSwipeToDismissBoxState()
    LaunchedEffect(state.currentValue) {
        if (state.currentValue == SwipeToDismissBoxValue.EndToStart) onRemove(word)
    }
    val removeLabel = stringResource(R.string.action_bookmark_remove)
    SwipeToDismissBox(
        state = state,
        modifier = modifier.semantics {
            customActions = listOf(
                CustomAccessibilityAction(removeLabel) {
                    onRemove(word)
                    true
                }
            )
        },
        enableDismissFromStartToEnd = false,
        backgroundContent = { RemoveBackground() }
    ) {
        SavedWordItem(
            word = word,
            onClick = { onClick(word) },
            modifier = Modifier.testTag(BookmarksTestTags.ITEM)
        )
    }
}

@Composable
private fun RemoveBackground() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.errorContainer)
            .padding(horizontal = 24.dp),
        contentAlignment = Alignment.CenterEnd
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_delete),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onErrorContainer
        )
    }
}

@Preview
@Composable
private fun BookmarksContentPreview() {
    val words = listOf(
        SavedWord(2, Dictionary.BelRus, 10, "прывітанне", "п"),
        SavedWord(1, Dictionary.RusBel, 11, "спасибо", "с")
    )
    SkarnikTheme {
        BookmarksContent(
            uiState = BookmarksUiState.Ready(words, hasMore = false, sortOrder = BookmarksSortOrder.DateAdded),
            snackbarHostState = remember { SnackbarHostState() },
            onWordClick = {},
            onRemove = {},
            onSortOrderChange = {},
            onLoadMore = {}
        )
    }
}
