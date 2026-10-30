package by.mazokaleh.skarnik.feature.search.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.clearText
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SearchBar
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import by.mazokaleh.skarnik.R
import by.mazokaleh.skarnik.core.model.Dictionary
import by.mazokaleh.skarnik.core.ui.nameRes
import by.mazokaleh.skarnik.feature.search.domain.SearchWord
import by.mazokaleh.skarnik.ui.theme.SkarnikTheme
import kotlinx.serialization.Serializable

@Serializable
data object SearchRoute

object SearchTestTags {
    const val FIELD = "search_field"
    const val CLEAR = "search_clear"
    const val HINT = "search_hint"
    const val NOTHING_FOUND = "search_nothing_found"
    const val ERROR = "search_error"
    const val RESULT = "search_result"
    const val LETTERS = "search_letters"
    fun letter(letter: String) = "search_letter_$letter"
}

/** Letters missing from one of the two keyboards: Russian on the left, Belarusian on the right. */
private val RUSSIAN_LETTERS = listOf("и", "щ", "ъ")
private val BELARUSIAN_LETTERS = listOf("і", "ў", "'")

@Composable
fun SearchScreen(
    onBack: () -> Unit,
    onWordClick: (SearchWord) -> Unit,
    viewModel: SearchViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val queryState = rememberTextFieldState()
    LaunchedEffect(queryState) {
        snapshotFlow { queryState.text.toString() }.collect(viewModel::onQueryChange)
    }
    SearchContent(uiState = uiState, queryState = queryState, onBack = onBack, onWordClick = onWordClick)
}

/**
 * M3: a full-screen expanded search bar with results under it, instead of Flutter's app bar with a text field.
 * The extra letters sit in a row above the keyboard instead of floating action buttons.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun SearchContent(
    uiState: SearchUiState,
    queryState: TextFieldState,
    onBack: () -> Unit,
    onWordClick: (SearchWord) -> Unit
) {
    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current
    // Open the keyboard once, not again when coming back from a translation.
    var focusedOnce by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        if (!focusedOnce) {
            focusRequester.requestFocus()
            focusedOnce = true
        }
    }

    SearchBar(
        inputField = {
            SearchBarDefaults.InputField(
                state = queryState,
                onSearch = { keyboardController?.hide() },
                expanded = true,
                onExpandedChange = { expanded -> if (!expanded) onBack() },
                modifier = Modifier.focusRequester(focusRequester).testTag(SearchTestTags.FIELD),
                placeholder = { Text(stringResource(R.string.search_placeholder)) },
                leadingIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            painter = painterResource(R.drawable.ic_arrow_back),
                            contentDescription = stringResource(R.string.action_back)
                        )
                    }
                },
                trailingIcon = {
                    if (queryState.text.isNotEmpty()) {
                        IconButton(
                            onClick = { queryState.clearText() },
                            modifier = Modifier.testTag(SearchTestTags.CLEAR)
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.ic_close),
                                contentDescription = stringResource(R.string.action_clear)
                            )
                        }
                    }
                }
            )
        },
        expanded = true,
        onExpandedChange = { expanded -> if (!expanded) onBack() }
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .windowInsetsPadding(
                    WindowInsets.ime.union(WindowInsets.navigationBars).only(WindowInsetsSides.Bottom)
                )
        ) {
            Box(Modifier.weight(1f).fillMaxWidth()) {
                SearchResults(uiState = uiState, onWordClick = onWordClick)
            }
            if (WindowInsets.isImeVisible) {
                ExtraLetters(onLetter = { queryState.insertAtCursor(it) })
            }
        }
    }
}

@Composable
private fun SearchResults(uiState: SearchUiState, onWordClick: (SearchWord) -> Unit) {
    when (uiState) {
        SearchUiState.Hint -> Message(stringResource(R.string.search_hint), SearchTestTags.HINT)
        is SearchUiState.NothingFound -> Message(
            stringResource(R.string.search_nothing_found),
            SearchTestTags.NOTHING_FOUND
        )
        SearchUiState.Error -> Message(stringResource(R.string.search_error), SearchTestTags.ERROR)
        is SearchUiState.Results -> LazyColumn(Modifier.fillMaxSize()) {
            items(uiState.words, key = { it.id }) { word ->
                ListItem(
                    headlineContent = { Text(word.word) },
                    supportingContent = { Text(stringResource(word.dictionary.nameRes)) },
                    // The expanded search bar already draws the container.
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                    modifier = Modifier
                        .clickable { onWordClick(word) }
                        .testTag(SearchTestTags.RESULT)
                )
            }
        }
    }
}

@Composable
private fun Message(text: String, tag: String) {
    Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
        Text(
            text = text,
            modifier = Modifier.testTag(tag),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun ExtraLetters(onLetter: (String) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .testTag(SearchTestTags.LETTERS),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        RUSSIAN_LETTERS.forEach { LetterButton(it, onLetter) }
        Spacer(Modifier.weight(1f))
        BELARUSIAN_LETTERS.forEach { LetterButton(it, onLetter) }
    }
}

@Composable
private fun LetterButton(letter: String, onLetter: (String) -> Unit) {
    val description = stringResource(R.string.search_insert_letter, letter)
    FilledTonalButton(
        onClick = { onLetter(letter) },
        modifier = Modifier
            .size(48.dp)
            .testTag(SearchTestTags.letter(letter))
            .semantics { contentDescription = description },
        contentPadding = PaddingValues(0.dp)
    ) {
        Text(letter, fontSize = 18.sp)
    }
}

/** Replaces the selection (or inserts at the cursor) with [text] and puts the cursor after it. */
internal fun TextFieldState.insertAtCursor(text: String) {
    edit {
        val start = selection.min
        replace(start, selection.max, text)
        selection = TextRange(start + text.length)
    }
}

@Preview
@Composable
private fun SearchContentPreview() {
    SkarnikTheme {
        SearchContent(
            uiState = SearchUiState.Results(
                query = "прыв",
                words = listOf(
                    SearchWord(1, Dictionary.BelRus, 10, "прывітанне", "п", "прывітанне"),
                    SearchWord(2, Dictionary.Tsbm, 11, "прывітанне", "п", "прывітанне")
                )
            ),
            queryState = rememberTextFieldState("прыв"),
            onBack = {},
            onWordClick = {}
        )
    }
}
