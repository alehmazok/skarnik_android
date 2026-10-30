package by.mazokaleh.skarnik.core.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import by.mazokaleh.skarnik.core.model.SavedWord

/**
 * A history or bookmarks list. When [hasMore], the last row asks for the next page as soon as it is composed,
 * i.e. when the user scrolls to the end.
 */
@Composable
fun SavedWordList(
    words: List<SavedWord>,
    hasMore: Boolean,
    onLoadMore: () -> Unit,
    modifier: Modifier = Modifier,
    itemContent: @Composable LazyItemScope.(SavedWord) -> Unit
) {
    LazyColumn(modifier = modifier.fillMaxSize(), state = rememberLazyListState()) {
        items(words, key = { it.id }) { itemContent(it) }
        if (hasMore) {
            item(key = LOAD_MORE_KEY, contentType = LOAD_MORE_KEY) {
                LaunchedEffect(words.size) { onLoadMore() }
                Box(Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
        }
    }
}

/** A row: the word and its dictionary, as in search results. */
@Composable
fun SavedWordItem(word: SavedWord, onClick: () -> Unit, modifier: Modifier = Modifier) {
    ListItem(
        headlineContent = { Text(word.word) },
        supportingContent = { Text(stringResource(word.dictionary.nameRes)) },
        modifier = modifier.clickable(onClick = onClick)
    )
}

/** Centered text for an empty list or an error. */
@Composable
fun ListMessage(text: String, modifier: Modifier = Modifier, isError: Boolean = false) {
    Box(modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
        Text(
            text = text,
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.bodyLarge,
            color = if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

private const val LOAD_MORE_KEY = "load_more"
