package by.mazokaleh.skarnik.feature.stress.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import by.mazokaleh.skarnik.R
import by.mazokaleh.skarnik.feature.stress.domain.StressSource
import by.mazokaleh.skarnik.feature.stress.domain.StressWordEntry
import by.mazokaleh.skarnik.ui.theme.SkarnikTheme

/**
 * Resolves a word to its stress entries (Flutter's `StressPage`). One entry replaces this screen with its
 * table ([onOpenTable] with `replace = true`); several are listed and a tap opens the table above the list.
 */
@Composable
fun StressScreen(
    route: StressRoute,
    onBack: () -> Unit,
    onOpenTable: (entry: StressWordEntry, replace: Boolean) -> Unit,
    viewModel: StressViewModel = hiltViewModel<StressViewModel, StressViewModel.Factory>(
        creationCallback = { it.create(route) }
    )
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val currentOnOpenTable by rememberUpdatedState(onOpenTable)
    LaunchedEffect(viewModel) {
        viewModel.openTable.collect { currentOnOpenTable(it, true) }
    }
    StressContent(uiState, onBack, onChoose = { onOpenTable(it, false) }, onRetry = viewModel::retry)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun StressContent(
    uiState: StressUiState,
    onBack: () -> Unit,
    onChoose: (StressWordEntry) -> Unit,
    onRetry: () -> Unit
) {
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = { StressTopBar(onBack, scrollBehavior) }
    ) { padding ->
        val modifier = Modifier.fillMaxSize().padding(padding)
        when (uiState) {
            StressUiState.Loading -> StressLoading(Modifier.padding(padding))
            is StressUiState.Choose -> ChooseList(uiState.words, onChoose, modifier)
            StressUiState.NotFound -> StressMessage(notFound = true, onRetry, modifier)
            StressUiState.Error -> StressMessage(notFound = false, onRetry, modifier)
        }
    }
}

@Composable
private fun ChooseList(words: List<StressWordEntry>, onChoose: (StressWordEntry) -> Unit, modifier: Modifier) {
    LazyColumn(modifier.windowInsetsPadding(WindowInsets.navigationBars.only(WindowInsetsSides.Bottom))) {
        item(key = "header") {
            Text(
                text = stringResource(R.string.stress_choose_word),
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 8.dp),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary
            )
        }
        // Ids are unique only within a source, and all entries here come from one source; the index keeps keys
        // unique even if a backend ever repeats an id.
        itemsIndexed(words, key = { index, entry -> "${entry.source}/${entry.id}/$index" }) { _, entry ->
            ListItem(
                headlineContent = {
                    // Maestro matches the text on the tagged Text: a merged ListItem node has none of its own.
                    Text(
                        entry.word,
                        Modifier.testTag(StressTestTags.CHOICE),
                        style = MaterialTheme.typography.titleLarge
                    )
                },
                modifier = Modifier.clickable { onChoose(entry) },
                supportingContent = partOfSpeech(entry.tableName)?.let { { Text(it) } }
            )
        }
    }
}

@Preview
@Composable
private fun StressChoosePreview() {
    val words = listOf(
        StressWordEntry(1, "пара", "па́ра", "Nouns", StressSource.Starnik),
        StressWordEntry(2, "пара", "пара́", "Nouns", StressSource.Starnik)
    )
    SkarnikTheme { StressContent(StressUiState.Choose(words), {}, {}, {}) }
}
