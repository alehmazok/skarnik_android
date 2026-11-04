package by.mazokaleh.skarnik.feature.translation.ui

import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.DisableSelection
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.onPlaced
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import by.mazokaleh.skarnik.R
import by.mazokaleh.skarnik.core.model.Dictionary
import by.mazokaleh.skarnik.core.model.Word
import by.mazokaleh.skarnik.core.model.WordLink
import by.mazokaleh.skarnik.core.settings.ThemeMode
import by.mazokaleh.skarnik.feature.review.ui.ReviewPromptDialog
import by.mazokaleh.skarnik.feature.review.ui.rememberReviewPromptState
import by.mazokaleh.skarnik.feature.stress.ui.StressButton
import by.mazokaleh.skarnik.feature.translation.domain.Translation
import by.mazokaleh.skarnik.ui.theme.SkarnikTheme
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable

/**
 * Opens a word's translation by id. [word] is shown while it loads (known when opened from a list).
 * [redirectedFrom] is the alias that led here; [saveToHistory] is false when opened from history or bookmarks.
 */
@Serializable
data class TranslationRoute(
    val langId: Long,
    val wordId: Long,
    val word: String? = null,
    val saveToHistory: Boolean = true,
    val redirectedFrom: String? = null
)

object TranslationTestTags {
    const val WORD = "translation_word"
    const val DICTIONARY = "translation_dictionary"
    const val TEXT = "translation_text"
    const val LOADING = "translation_loading"
    const val ERROR = "translation_error"
    const val RETRY = "translation_retry"
    const val BOOKMARK = "translation_bookmark"
    const val SHARE = "translation_share"
}

/** [onBack] is null for a word opened beside a list (wide screens): the list is the way back, so no back arrow. */
@Composable
fun TranslationScreen(
    route: TranslationRoute,
    onBack: (() -> Unit)?,
    onOpenWord: (WordLink) -> Unit,
    onRedirect: (target: WordLink, fromWord: String) -> Unit,
    onStress: (word: String) -> Unit,
    viewModel: TranslationViewModel = hiltViewModel<TranslationViewModel, TranslationViewModel.Factory>(
        creationCallback = { it.create(route) }
    )
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val reviewPrompt = rememberReviewPromptState()
    TranslationEffects(viewModel, route.redirectedFrom, snackbarHostState, onRedirect, reviewPrompt::ask)
    ReviewPromptDialog(reviewPrompt)

    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current
    TranslationContent(
        uiState = uiState,
        snackbarHostState = snackbarHostState,
        onBack = onBack,
        onToggleBookmark = viewModel::toggleBookmark,
        onShare = {
            viewModel.onShare(it)
            context.shareTranslation(it)
        },
        onRetry = viewModel::retry,
        onStress = onStress,
        onLink = { href ->
            val link = WordLink.parse(href)
            when {
                link != null -> onOpenWord(link)
                // Links to other sites open in the browser; Flutter ignored them.
                href.startsWith("http://") || href.startsWith("https://") -> {
                    try {
                        uriHandler.openUri(href)
                    } catch (e: IllegalArgumentException) {
                        Log.w("TranslationScreen", "No app opens $href", e)
                    }
                }
            }
        }
    )
}

/** One-off events: redirects, snackbars and the review prompt. */
@Composable
private fun TranslationEffects(
    viewModel: TranslationViewModel,
    redirectedFrom: String?,
    snackbarHostState: SnackbarHostState,
    onRedirect: (target: WordLink, fromWord: String) -> Unit,
    onAskForReview: () -> Unit
) {
    val resources = LocalResources.current
    val currentOnRedirect by rememberUpdatedState(onRedirect)
    val currentOnAskForReview by rememberUpdatedState(onAskForReview)

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                is TranslationEvent.Redirect -> currentOnRedirect(event.target, event.fromWord)
                is TranslationEvent.BookmarkChanged -> {
                    val text = if (event.bookmarked) {
                        R.string.translation_bookmark_added
                    } else {
                        R.string.translation_bookmark_removed
                    }
                    snackbarHostState.currentSnackbarData?.dismiss()
                    // Not awaited, so a quick second tap replaces the snackbar instead of queueing behind it.
                    launch { snackbarHostState.showSnackbar(resources.getString(text)) }
                }
                TranslationEvent.AskForReview -> currentOnAskForReview()
            }
        }
    }
    // Flutter showed this for 15 s on the screen it pushed; here it shows once on the target word.
    var redirectShown by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(redirectedFrom) {
        val from = redirectedFrom
        if (from != null && !redirectShown) {
            redirectShown = true
            snackbarHostState.showSnackbar(
                resources.getString(R.string.translation_redirected_from, from),
                duration = SnackbarDuration.Long
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TranslationContent(
    uiState: TranslationUiState,
    snackbarHostState: SnackbarHostState,
    onBack: (() -> Unit)?,
    onToggleBookmark: () -> Unit,
    onShare: (Translation) -> Unit,
    onRetry: () -> Unit,
    onStress: (String) -> Unit,
    onLink: (String) -> Unit
) {
    // M3: the bar takes the scrolled-under colour once content slides beneath it, and shows the word once the
    // headline has scrolled out of view.
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    val scrollState = rememberScrollState()
    var titleBottom by remember { mutableIntStateOf(Int.MAX_VALUE) }
    val showBarTitle by remember { derivedStateOf { scrollState.value > titleBottom } }
    val onTitlePlaced: (Int) -> Unit = remember { { bottom: Int -> titleBottom = bottom } }
    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            TranslationTopBar(
                uiState = uiState,
                showTitle = { showBarTitle },
                scrollBehavior = scrollBehavior,
                onBack = onBack,
                onToggleBookmark = onToggleBookmark,
                onShare = onShare
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(scrollState)
                .windowInsetsPadding(WindowInsets.navigationBars.only(WindowInsetsSides.Bottom))
                .padding(horizontal = 16.dp)
                .padding(bottom = 24.dp)
        ) {
            // A comfortable line length on tablets and unfolded foldables.
            Column(Modifier.widthIn(max = MAX_TEXT_WIDTH)) {
                when (uiState) {
                    is TranslationUiState.Loading -> LoadingContent(uiState.word, onTitlePlaced)
                    is TranslationUiState.Error -> ErrorContent(uiState.word, onTitlePlaced, onRetry)
                    is TranslationUiState.Loaded -> LoadedContent(uiState, onTitlePlaced, onStress, onLink)
                }
            }
        }
    }
}

private val MAX_TEXT_WIDTH = 640.dp

@Composable
private fun LoadingContent(word: String?, onTitlePlaced: (Int) -> Unit) {
    if (word != null) Title(word, onTitlePlaced)
    LinearProgressIndicator(
        Modifier
            .fillMaxWidth()
            .padding(top = 16.dp)
            .testTag(TranslationTestTags.LOADING)
    )
}

@Composable
private fun ErrorContent(word: String?, onTitlePlaced: (Int) -> Unit, onRetry: () -> Unit) {
    if (word != null) Title(word, onTitlePlaced)
    Column(
        Modifier.fillMaxWidth().padding(top = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = stringResource(R.string.translation_error),
            modifier = Modifier.testTag(TranslationTestTags.ERROR),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        FilledTonalButton(onClick = onRetry, modifier = Modifier.testTag(TranslationTestTags.RETRY)) {
            Text(stringResource(R.string.action_retry))
        }
    }
}

/** The headword and the translation share one selection, so both can be selected and copied in one go. */
@Composable
private fun LoadedContent(
    uiState: TranslationUiState.Loaded,
    onTitlePlaced: (Int) -> Unit,
    onStress: (String) -> Unit,
    onLink: (String) -> Unit
) {
    val translation = uiState.translation
    SelectionContainer {
        Column {
            // Neutral, so red stays for the words of the translation.
            Text(
                text = stringResource(translation.word.dictionary.translationNameRes),
                modifier = Modifier.testTag(TranslationTestTags.DICTIONARY),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(4.dp))
            Title(translation.title, onTitlePlaced)
            DisableSelection {
                StressButton(uiState.stressCandidates, onStress, Modifier.padding(top = 8.dp))
            }
            Spacer(Modifier.height(16.dp))
            val colors = rememberTranslationColors()
            val currentOnLink by rememberUpdatedState(onLink)
            val text = remember(translation.html, colors) {
                translationHtmlToAnnotatedString(translation.html, colors) { currentOnLink(it) }
                    .withHangingIndent(HANGING_INDENT)
            }
            Text(
                text = text,
                modifier = Modifier.testTag(TranslationTestTags.TEXT),
                style = MaterialTheme.typography.bodyLarge
            )
        }
    }
}

private val HANGING_INDENT = 16.sp

/** Reports the bottom of the headline (in scroll content pixels), so the top app bar knows when to show the word. */
@Composable
private fun Title(word: String, onPlaced: (Int) -> Unit) {
    Text(
        text = word,
        modifier = Modifier
            .testTag(TranslationTestTags.WORD)
            .onPlaced { onPlaced(it.positionInParent().y.toInt() + it.size.height) },
        style = MaterialTheme.typography.headlineMedium
    )
}

private val Dictionary.translationNameRes: Int
    get() = when (this) {
        Dictionary.BelRus -> R.string.translation_into_russian
        Dictionary.RusBel -> R.string.translation_into_belarusian
        Dictionary.Tsbm -> R.string.dictionary_tsbm
    }

/** The share sheet with the skarnik.app link, the word as subject (Flutter: `SharePlus` with `subject`). */
private fun Context.shareTranslation(translation: Translation) {
    val send = Intent(Intent.ACTION_SEND)
        .setType("text/plain")
        .putExtra(Intent.EXTRA_TEXT, translation.shareUrl)
        .putExtra(Intent.EXTRA_SUBJECT, translation.word.word)
    startActivity(Intent.createChooser(send, null))
}

/** Both palettes and both themes: the brand palette once fell back to the M3 baseline purple unnoticed. */
private class PaletteProvider : PreviewParameterProvider<Pair<Boolean, ThemeMode>> {
    override val values = sequenceOf(
        true to ThemeMode.Light,
        false to ThemeMode.Light,
        false to ThemeMode.Dark
    )
}

@Preview
@Composable
private fun TranslationContentPreview(@PreviewParameter(PaletteProvider::class) palette: Pair<Boolean, ThemeMode>) {
    val word = Word(Dictionary.BelRus, 1, "бурштын", "б", "бурштын", null)
    SkarnikTheme(themeMode = palette.second, useDynamicColor = palette.first) {
        TranslationContent(
            uiState = TranslationUiState.Loaded(
                translation = Translation(
                    word = word,
                    stress = "буршты́н",
                    html = "янтарь — &nbsp;<font size=\"+2\" color=\"#831b03\">янтар</font>, " +
                        "<i>-ру <font color=\"#008000\">муж.</font></i> <a href=\"/belrus/2\">гл.</a><br>" +
                        "сукцинит — <font color=\"#831b03\">бурштын</font>, " +
                        "<i>-ну <font color=\"#008000\">муж.</font></i>",
                    source = Translation.Source.Api
                ),
                bookmarked = true,
                stressCandidates = listOf("янтар", "бурштын")
            ),
            snackbarHostState = remember { SnackbarHostState() },
            onBack = {},
            onToggleBookmark = {},
            onShare = {},
            onRetry = {},
            onStress = {},
            onLink = {}
        )
    }
}
