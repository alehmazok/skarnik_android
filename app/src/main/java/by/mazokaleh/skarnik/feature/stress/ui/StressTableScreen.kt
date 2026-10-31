package by.mazokaleh.skarnik.feature.stress.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import by.mazokaleh.skarnik.feature.stress.domain.StressRow
import by.mazokaleh.skarnik.feature.stress.domain.StressSource
import by.mazokaleh.skarnik.feature.translation.ui.TranslationColors
import by.mazokaleh.skarnik.feature.translation.ui.rememberTranslationColors
import by.mazokaleh.skarnik.feature.translation.ui.translationHtmlToAnnotatedString
import by.mazokaleh.skarnik.ui.theme.SkarnikTheme

/** A stress table (Flutter's `StressTablePage`): the stressed word, its part of speech and the table. */
@Composable
fun StressTableScreen(
    route: StressTableRoute,
    onBack: () -> Unit,
    viewModel: StressTableViewModel = hiltViewModel<StressTableViewModel, StressTableViewModel.Factory>(
        creationCallback = { it.create(route) }
    )
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    StressTableContent(route, uiState, onBack, viewModel::retry)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun StressTableContent(
    route: StressTableRoute,
    uiState: StressTableUiState,
    onBack: () -> Unit,
    onRetry: () -> Unit
) {
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = { StressTopBar(onBack, scrollBehavior) }
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .windowInsetsPadding(WindowInsets.navigationBars.only(WindowInsetsSides.Bottom))
                .padding(bottom = 24.dp)
        ) {
            Header(route)
            when (uiState) {
                StressTableUiState.Loading -> StressLoading(Modifier.padding(horizontal = 16.dp))
                is StressTableUiState.Loaded -> StressTable(uiState.rows)
                StressTableUiState.NotFound -> StressMessage(notFound = true, onRetry)
                StressTableUiState.Error -> StressMessage(notFound = false, onRetry)
            }
        }
    }
}

@Composable
private fun Header(route: StressTableRoute) {
    Column(Modifier.padding(horizontal = 16.dp).padding(bottom = 16.dp)) {
        partOfSpeech(route.tableName)?.let {
            Text(
                text = it,
                modifier = Modifier.testTag(StressTestTags.PART_OF_SPEECH),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.height(4.dp))
        }
        SelectionContainer {
            Text(
                text = route.word,
                modifier = Modifier.testTag(StressTestTags.WORD),
                style = MaterialTheme.typography.headlineMedium
            )
        }
    }
}

/**
 * Two columns, form name | forms, with dividers between rows and columns. M3 has no data table component, so it
 * is a column of rows; the cells are the HTML of the source, styled like translations.
 */
@Composable
private fun StressTable(rows: List<StressRow>) {
    val colors = rememberTranslationColors()
    SelectionContainer {
        Column(Modifier.fillMaxWidth()) {
            HorizontalDivider()
            rows.forEach { row ->
                Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
                    Cell(row.title, colors, Modifier.weight(TITLE_WEIGHT))
                    VerticalDivider(Modifier.fillMaxHeight())
                    Cell(row.content, colors, Modifier.weight(CONTENT_WEIGHT))
                }
                HorizontalDivider()
            }
        }
    }
}

@Composable
private fun Cell(html: String, colors: TranslationColors, modifier: Modifier) {
    // Stress tables have no links.
    val text = remember(html, colors) { translationHtmlToAnnotatedString(html, colors) {} }
    Text(
        text = text,
        // Tagged per cell: the a11y tree (and so Maestro) only shows nodes with text.
        modifier = modifier.padding(horizontal = 16.dp, vertical = 8.dp).testTag(StressTestTags.CELL),
        style = MaterialTheme.typography.bodyLarge
    )
}

// Flutter's column flex 2 : 3.
private const val TITLE_WEIGHT = 2f
private const val CONTENT_WEIGHT = 3f

@Preview
@Composable
private fun StressTablePreview() {
    val rows = listOf(
        StressRow(
            "Назоўны<br><span class=\"pytanne\">хто? што?</span>",
            "<b> рука́ </b><br><br>" +
                "<span class=\"skarot\">мн.</span>&nbsp;<b> ру́кі </b>"
        ),
        StressRow("Родны<br><span class=\"pytanne\">каго? чаго?</span>", "<b> рукі́ </b>")
    )
    SkarnikTheme {
        StressTableContent(
            StressTableRoute(1, StressSource.Admin, "рука́", "Nouns"),
            StressTableUiState.Loaded(rows),
            {},
            {}
        )
    }
}
