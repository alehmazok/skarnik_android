package by.mazokaleh.skarnik.feature.stress.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import by.mazokaleh.skarnik.R
import by.mazokaleh.skarnik.feature.stress.domain.StressPickerRow
import by.mazokaleh.skarnik.feature.stress.domain.StressPickerSection
import by.mazokaleh.skarnik.feature.stress.domain.StressSource
import by.mazokaleh.skarnik.feature.stress.domain.StressWordEntry
import by.mazokaleh.skarnik.feature.stress.domain.stressPickerSections
import by.mazokaleh.skarnik.ui.theme.SkarnikTheme

/**
 * Resolves a word to its stress entries (Flutter's `StressPage`). One entry replaces this screen with its
 * table ([onOpenTable] with `replace = true`); several are offered in the homonym picker and a tap opens the table
 * above it.
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
            is StressUiState.Choose -> ChooseList(uiState, onChoose, modifier)
            StressUiState.NotFound -> StressMessage(notFound = true, onRetry, modifier)
            StressUiState.Error -> StressMessage(notFound = false, onRetry, modifier)
        }
    }
}

/**
 * The homonym picker (skarnik_ios#102): entries grouped by stressed form, each row its meaning and part of speech.
 * One form only (same stress everywhere, e.g. «а») shows a banner instead of section headers. M3: a full screen,
 * not iOS's sheet, since loading, not found and retry already live here.
 */
@Composable
private fun ChooseList(choose: StressUiState.Choose, onChoose: (StressWordEntry) -> Unit, modifier: Modifier) {
    val single = choose.sections.singleOrNull()
    LazyColumn(modifier.windowInsetsPadding(WindowInsets.navigationBars.only(WindowInsetsSides.Bottom))) {
        item(key = "header") {
            Text(
                text = stringResource(R.string.stress_choose_word),
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 8.dp),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary
            )
        }
        if (single != null) {
            item(key = "banner") { SameStressBanner(single.word) }
        }
        choose.sections.forEach { section ->
            if (single == null) {
                item(key = "section/${section.word}") { SectionHeader(section) }
            }
            // Ids are unique only within a source, and all entries here come from one source; the index keeps
            // keys unique even if a backend ever repeats an id.
            itemsIndexed(
                section.rows,
                key = { index, row -> "${section.word}/${row.entry.source}/${row.entry.id}/$index" }
            ) { _, row ->
                ChoiceRow(row, onChoose)
            }
        }
        if (choose.noMeanings) {
            item(key = "no_meanings") {
                Text(
                    text = stringResource(R.string.stress_no_meanings),
                    modifier = Modifier
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                        .testTag(StressTestTags.NO_MEANINGS),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun SameStressBanner(word: String) {
    val accent = MaterialTheme.colorScheme.primary
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .semantics(mergeDescendants = true) {}
            .testTag(StressTestTags.BANNER),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHighest)
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                text = remember(word, accent) { highlightStress(word, accent) },
                style = MaterialTheme.typography.headlineLarge,
                maxLines = 1,
                autoSize = TextAutoSize.StepBased(minFontSize = 16.sp, maxFontSize = BANNER_MAX_FONT)
            )
            Text(
                text = stringResource(R.string.stress_same_stress),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun SectionHeader(section: StressPickerSection) {
    val accent = MaterialTheme.colorScheme.primary
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 4.dp)
            .semantics(mergeDescendants = true) { heading() }
            .testTag(StressTestTags.SECTION),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(
            text = remember(section.word, accent) { highlightStress(section.word, accent) },
            modifier = Modifier.alignByBaseline(),
            style = MaterialTheme.typography.headlineSmall
        )
        if (section.meaningCount > 0) {
            Text(
                text = pluralStringResource(R.plurals.stress_meaning_count, section.meaningCount, section.meaningCount),
                modifier = Modifier.alignByBaseline(),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/** TalkBack reads the row's texts; the section header (a heading) or the banner above it names the word. */
@Composable
private fun ChoiceRow(row: StressPickerRow, onChoose: (StressWordEntry) -> Unit) {
    val entry = row.entry
    val partOfSpeech = partOfSpeech(entry.tableName)
    val title = entry.meaning ?: partOfSpeech ?: entry.word
    val supporting = if (entry.meaning != null) partOfSpeech else null
    ListItem(
        headlineContent = {
            // Maestro matches the text on the tagged Text: a merged ListItem node has none of its own.
            Text(
                title,
                Modifier.testTag(StressTestTags.CHOICE),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        },
        modifier = Modifier
            .heightIn(min = 56.dp)
            .clickable { onChoose(entry) },
        supportingContent = supporting?.let { { Text(it) } },
        trailingContent = row.ordinal?.let {
            {
                Text(
                    it.toString(),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    )
}

private val BANNER_MAX_FONT = 32.sp

@Preview
@Composable
private fun StressChoosePreview() {
    val words = listOf(
        StressWordEntry(1, "пара", "па́ра", "Nouns", StressSource.Starnik, "два аднолькавыя прадметы"),
        StressWordEntry(2, "пара", "пара́", "Nouns", StressSource.Starnik, "час, пара года"),
        StressWordEntry(3, "пара", "па́ра", "Nouns", StressSource.Starnik, "газападобны стан вады")
    )
    SkarnikTheme { StressContent(StressUiState.Choose(stressPickerSections(words)), {}, {}, {}) }
}

@Preview
@Composable
private fun StressSameStressPreview() {
    val words = listOf(
        StressWordEntry(1, "а", "а́", "Nouns", StressSource.Admin),
        StressWordEntry(2, "а", "а́", "Conjunctions", StressSource.Admin),
        StressWordEntry(3, "а", "а́", "Conjunctions", StressSource.Admin)
    )
    SkarnikTheme { StressContent(StressUiState.Choose(stressPickerSections(words)), {}, {}, {}) }
}
