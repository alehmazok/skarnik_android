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
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import by.mazokaleh.skarnik.R
import by.mazokaleh.skarnik.core.model.Dictionary
import by.mazokaleh.skarnik.core.model.Word
import by.mazokaleh.skarnik.core.model.WordLink
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

@Composable
fun TranslationScreen(
    route: TranslationRoute,
    onBack: () -> Unit,
    onOpenWord: (WordLink) -> Unit,
    onRedirect: (target: WordLink, fromWord: String) -> Unit,
    viewModel: TranslationViewModel = hiltViewModel<TranslationViewModel, TranslationViewModel.Factory>(
        creationCallback = { it.create(route) }
    )
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    TranslationEffects(viewModel, route.redirectedFrom, snackbarHostState, onRedirect)

    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current
    TranslationContent(
        uiState = uiState,
        snackbarHostState = snackbarHostState,
        onBack = onBack,
        onToggleBookmark = viewModel::toggleBookmark,
        onShare = { context.shareTranslation(it) },
        onRetry = viewModel::retry,
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

/** One-off events: redirects and snackbars. */
@Composable
private fun TranslationEffects(
    viewModel: TranslationViewModel,
    redirectedFrom: String?,
    snackbarHostState: SnackbarHostState,
    onRedirect: (target: WordLink, fromWord: String) -> Unit
) {
    val resources = LocalResources.current
    val currentOnRedirect by rememberUpdatedState(onRedirect)

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
    onBack: () -> Unit,
    onToggleBookmark: () -> Unit,
    onShare: (Translation) -> Unit,
    onRetry: () -> Unit,
    onLink: (String) -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {},
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            painter = painterResource(R.drawable.ic_arrow_back),
                            contentDescription = stringResource(R.string.action_back)
                        )
                    }
                },
                actions = {
                    if (uiState is TranslationUiState.Loaded) {
                        TranslationActions(uiState, onToggleBookmark, onShare)
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .windowInsetsPadding(WindowInsets.navigationBars.only(WindowInsetsSides.Bottom))
                .padding(horizontal = 16.dp)
                .padding(bottom = 24.dp)
        ) {
            when (uiState) {
                is TranslationUiState.Loading -> LoadingContent(uiState.word)
                is TranslationUiState.Error -> ErrorContent(uiState.word, onRetry)
                is TranslationUiState.Loaded -> LoadedContent(uiState.translation, onLink)
            }
        }
    }
}

@Composable
private fun TranslationActions(
    uiState: TranslationUiState.Loaded,
    onToggleBookmark: () -> Unit,
    onShare: (Translation) -> Unit
) {
    IconButton(onClick = onToggleBookmark, modifier = Modifier.testTag(TranslationTestTags.BOOKMARK)) {
        if (uiState.bookmarked) {
            Icon(
                painter = painterResource(R.drawable.ic_bookmark),
                contentDescription = stringResource(R.string.action_bookmark_remove),
                tint = MaterialTheme.colorScheme.primary
            )
        } else {
            Icon(
                painter = painterResource(R.drawable.ic_bookmark_border),
                contentDescription = stringResource(R.string.action_bookmark_add)
            )
        }
    }
    IconButton(
        onClick = { onShare(uiState.translation) },
        modifier = Modifier.testTag(TranslationTestTags.SHARE)
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_share),
            contentDescription = stringResource(R.string.action_share)
        )
    }
}

@Composable
private fun LoadingContent(word: String?) {
    if (word != null) Title(word)
    LinearProgressIndicator(
        Modifier
            .fillMaxWidth()
            .padding(top = 16.dp)
            .testTag(TranslationTestTags.LOADING)
    )
}

@Composable
private fun ErrorContent(word: String?, onRetry: () -> Unit) {
    if (word != null) Title(word)
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

@Composable
private fun LoadedContent(translation: Translation, onLink: (String) -> Unit) {
    Text(
        text = stringResource(translation.word.dictionary.translationNameRes),
        modifier = Modifier.testTag(TranslationTestTags.DICTIONARY),
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary
    )
    Spacer(Modifier.height(4.dp))
    Title(translation.title)
    Spacer(Modifier.height(16.dp))
    val colors = rememberTranslationColors()
    val currentOnLink by rememberUpdatedState(onLink)
    val text = remember(translation.html, colors) {
        translationHtmlToAnnotatedString(translation.html, colors) { currentOnLink(it) }
    }
    SelectionContainer {
        Text(
            text = text,
            modifier = Modifier.testTag(TranslationTestTags.TEXT),
            style = MaterialTheme.typography.bodyLarge
        )
    }
}

@Composable
private fun Title(word: String) {
    SelectionContainer {
        Text(
            text = word,
            modifier = Modifier.testTag(TranslationTestTags.WORD),
            style = MaterialTheme.typography.headlineMedium
        )
    }
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

@Preview
@Composable
private fun TranslationContentPreview() {
    val word = Word(Dictionary.BelRus, 1, "бурштын", "б", "бурштын", null)
    SkarnikTheme {
        TranslationContent(
            uiState = TranslationUiState.Loaded(
                translation = Translation(
                    word = word,
                    stress = "буршты́н",
                    html = "<font size=\"+2\" color=\"#831b03\">янтар</font>, " +
                        "<i>-ру <font color=\"#008000\">муж.</font></i> <a href=\"/belrus/2\">гл.</a>",
                    source = Translation.Source.Api
                ),
                bookmarked = true
            ),
            snackbarHostState = remember { SnackbarHostState() },
            onBack = {},
            onToggleBookmark = {},
            onShare = {},
            onRetry = {},
            onLink = {}
        )
    }
}
