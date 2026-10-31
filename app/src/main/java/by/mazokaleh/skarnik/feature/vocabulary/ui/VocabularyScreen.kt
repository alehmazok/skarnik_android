package by.mazokaleh.skarnik.feature.vocabulary.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import by.mazokaleh.skarnik.R
import by.mazokaleh.skarnik.core.model.Dictionary
import by.mazokaleh.skarnik.core.ui.shortNameRes
import by.mazokaleh.skarnik.feature.vocabulary.domain.Vocabulary
import by.mazokaleh.skarnik.feature.vocabulary.domain.VocabularySection
import by.mazokaleh.skarnik.feature.vocabulary.domain.VocabularyWord
import by.mazokaleh.skarnik.ui.theme.SkarnikTheme
import kotlinx.coroutines.launch

object VocabularyTestTags {
    const val TAB_PREFIX = "vocabulary_tab_"
    const val LIST = "vocabulary_list"
    const val ITEM = "vocabulary_item"
    const val HEADER = "vocabulary_header"
    const val INDEX = "vocabulary_index"

    /** Each letter of the index; it carries both this tag and the letter, so match them together. */
    const val INDEX_LETTER = "vocabulary_index_letter"
    const val LOADING = "vocabulary_loading"
    const val ERROR = "vocabulary_error"
    const val RETRY = "vocabulary_retry"
}

/** The Dictionary tab: one dictionary at a time, alphabetical, with a letter index to jump. */
@Composable
fun VocabularyScreen(
    onWordClick: (Dictionary, VocabularyWord) -> Unit,
    viewModel: VocabularyViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    VocabularyPage(uiState, viewModel::select, viewModel::retry) { dictionary, word ->
        viewModel.onWordClick(dictionary, word)
        onWordClick(dictionary, word)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun VocabularyPage(
    uiState: VocabularyUiState,
    onSelect: (Dictionary) -> Unit,
    onRetry: () -> Unit,
    onWordClick: (Dictionary, VocabularyWord) -> Unit
) {
    // Each dictionary keeps its own scroll position while another one shows.
    val listStates = rememberSaveableStateHolder()
    Scaffold(
        topBar = {
            Column {
                TopAppBar(title = { Text(stringResource(R.string.vocabulary_title)) })
                DictionaryTabs(uiState.dictionary, onSelect)
            }
        }
    ) { padding ->
        val modifier = Modifier.padding(padding)
        when (val content = uiState.content) {
            VocabularyContent.Loading -> Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(Modifier.testTag(VocabularyTestTags.LOADING))
            }
            is VocabularyContent.Ready -> listStates.SaveableStateProvider(uiState.dictionary) {
                VocabularyList(content.vocabulary, onWordClick = { onWordClick(uiState.dictionary, it) }, modifier)
            }
            VocabularyContent.Error -> VocabularyError(onRetry, modifier)
        }
    }
}

@Composable
private fun DictionaryTabs(selected: Dictionary, onSelect: (Dictionary) -> Unit) {
    PrimaryTabRow(selectedTabIndex = VOCABULARY_DICTIONARIES.indexOf(selected)) {
        VOCABULARY_DICTIONARIES.forEach { dictionary ->
            Tab(
                selected = dictionary == selected,
                onClick = { onSelect(dictionary) },
                text = { Text(stringResource(dictionary.shortNameRes)) },
                modifier = Modifier.testTag(VocabularyTestTags.TAB_PREFIX + dictionary.path)
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun VocabularyList(vocabulary: Vocabulary, onWordClick: (VocabularyWord) -> Unit, modifier: Modifier) {
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    var activeLetter by remember { mutableStateOf<String?>(null) }
    val indexLetters = remember(vocabulary) { vocabulary.indexSections.map { it.letter } }

    Box(modifier.fillMaxSize()) {
        Row(Modifier.fillMaxSize()) {
            LazyColumn(Modifier.weight(1f).fillMaxHeight().testTag(VocabularyTestTags.LIST), state = listState) {
                vocabulary.sections.forEach { section ->
                    stickyHeader(key = HEADER_KEY_PREFIX + section.letter, contentType = HEADER_KEY_PREFIX) {
                        SectionHeader(section.letter)
                    }
                    items(section.words, key = { it.wordId }, contentType = { WORD_CONTENT_TYPE }) { word ->
                        ListItem(
                            headlineContent = { Text(word.word) },
                            modifier = Modifier
                                .clickable { onWordClick(word) }
                                .testTag(VocabularyTestTags.ITEM)
                        )
                    }
                }
            }
            LetterIndexBar(
                letters = indexLetters,
                onLetter = { index ->
                    val section = vocabulary.indexSections[index]
                    activeLetter = section.letter
                    scope.launch { listState.scrollToItem(section.firstRow) }
                },
                onRelease = { activeLetter = null },
                modifier = Modifier.fillMaxHeight().testTag(VocabularyTestTags.INDEX)
            )
        }
        activeLetter?.let { LetterBubble(it, Modifier.align(Alignment.Center)) }
    }
}

@Composable
private fun SectionHeader(letter: String) {
    Text(
        text = letter,
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .testTag(VocabularyTestTags.HEADER),
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary
    )
}

/** The letter under the finger while the index is dragged, large enough to read past the thumb. */
@Composable
private fun LetterBubble(letter: String, modifier: Modifier = Modifier) {
    Surface(modifier.size(72.dp), shape = CircleShape, color = MaterialTheme.colorScheme.primaryContainer) {
        Box(contentAlignment = Alignment.Center) {
            Text(letter, style = MaterialTheme.typography.headlineMedium)
        }
    }
}

@Composable
private fun VocabularyError(onRetry: () -> Unit, modifier: Modifier) {
    Column(
        modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = stringResource(R.string.database_error),
            modifier = Modifier.testTag(VocabularyTestTags.ERROR),
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.error
        )
        Button(onClick = onRetry, modifier = Modifier.padding(top = 16.dp).testTag(VocabularyTestTags.RETRY)) {
            Text(stringResource(R.string.action_retry))
        }
    }
}

private const val HEADER_KEY_PREFIX = "header_"
private const val WORD_CONTENT_TYPE = "word"

@Preview
@Composable
private fun VocabularyPagePreview() {
    val vocabulary = Vocabulary(
        listOf(
            VocabularySection("А", listOf(VocabularyWord(1, "абавязак"), VocabularyWord(2, "аблокі")), 0, true),
            VocabularySection("Б", listOf(VocabularyWord(3, "бацька")), 3, true)
        )
    )
    SkarnikTheme {
        VocabularyPage(VocabularyUiState(Dictionary.Tsbm, VocabularyContent.Ready(vocabulary)), {}, {}, { _, _ -> })
    }
}
