package by.mazokaleh.skarnik.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.Stable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import by.mazokaleh.skarnik.R
import by.mazokaleh.skarnik.core.model.Dictionary
import by.mazokaleh.skarnik.core.model.WordLink
import by.mazokaleh.skarnik.core.ui.WindowWidthClass
import by.mazokaleh.skarnik.feature.stress.ui.stressDestinations
import by.mazokaleh.skarnik.feature.translation.ui.TranslationRoute
import kotlinx.serialization.Serializable

/** The empty translation pane: nothing opened from the list yet. */
@Serializable
data object TranslationPlaceholderRoute

object TranslationPaneTestTags {
    const val PANE = "translation_pane"
    const val PLACEHOLDER = "translation_placeholder"
}

/** M3 list pane width in an expanded window; the translation takes the rest. */
private val ListPaneWidth = 360.dp
private const val LIST_STATE_KEY = "list"

/**
 * The word opened from one list on a wide screen (parity 0.5). The pane has its own back stack, so links,
 * redirects and the stress screens open inside it. [openWord] is the row it was opened from, highlighted in the
 * list; it is cleared when back returns the pane to its placeholder.
 */
@Stable
class TranslationPaneState internal constructor(
    internal val navController: NavHostController,
    private val openWordState: MutableState<WordLink?>
) {
    val openWord: WordLink? get() = openWordState.value

    /** Shows [route] in the pane in place of whatever was open there. */
    fun open(route: TranslationRoute) {
        openWordState.value = Dictionary.byLangId(route.langId)?.let { WordLink(it, route.wordId) }
        navController.navigate(route) { popUpTo<TranslationPlaceholderRoute>() }
    }

    internal fun onPlaceholderShown() {
        openWordState.value = null
    }
}

@Composable
fun rememberTranslationPaneState(): TranslationPaneState {
    val navController = rememberNavController()
    val openWord = rememberSaveable(stateSaver = OpenWordSaver) { mutableStateOf<WordLink?>(null) }
    val state = remember(navController) { TranslationPaneState(navController, openWord) }
    LaunchedEffect(state) {
        navController.currentBackStackEntryFlow.collect { entry ->
            if (entry.destination.hasRoute<TranslationPlaceholderRoute>()) state.onPlaceholderShown()
        }
    }
    return state
}

private val OpenWordSaver = Saver<WordLink?, LongArray>(
    save = { link -> link?.let { longArrayOf(it.dictionary.langId, it.wordId) } },
    restore = { saved -> Dictionary.byLangId(saved[0])?.let { WordLink(it, saved[1]) } }
)

/**
 * A list whose rows open words: [list] gets `open` for a row tap and the word open beside it.
 *
 * Below [WindowWidthClass.Expanded] a tap opens the word full screen through [onOpenFullScreen], as on phones.
 * From it the word opens in [pane], beside the list. A word still open when the window narrows (a foldable
 * folded, a tablet rotated) takes the whole space until back returns to the list, as M3's list–detail does.
 */
@Composable
fun ListDetailLayout(
    widthClass: WindowWidthClass,
    pane: TranslationPaneState,
    onOpenFullScreen: (TranslationRoute) -> Unit,
    list: @Composable (open: (TranslationRoute) -> Unit, openWord: WordLink?) -> Unit
) {
    val twoPanes = widthClass.hasListDetail
    val openWord = pane.openWord
    val currentOnOpenFullScreen by rememberUpdatedState(onOpenFullScreen)
    val open: (TranslationRoute) -> Unit = remember(pane, twoPanes) {
        { route -> if (twoPanes) pane.open(route) else currentOnOpenFullScreen(route) }
    }
    // The list keeps its scroll position while a narrowed window shows only the word.
    val listStates = rememberSaveableStateHolder()
    // The first word has a back arrow only when the list is not beside it.
    val firstWordHasBack = rememberUpdatedState(!twoPanes)

    Row(Modifier.fillMaxSize()) {
        if (twoPanes || openWord == null) {
            val listModifier = if (twoPanes) {
                Modifier.width(ListPaneWidth).consumeWindowInsets(WindowInsets.safeDrawing.only(WindowInsetsSides.End))
            } else {
                Modifier.weight(1f)
            }
            Box(listModifier.fillMaxHeight()) {
                listStates.SaveableStateProvider(LIST_STATE_KEY) { list(open, if (twoPanes) openWord else null) }
            }
        }
        if (twoPanes) VerticalDivider()
        if (twoPanes || openWord != null) {
            val paneModifier = if (twoPanes) {
                Modifier.consumeWindowInsets(WindowInsets.safeDrawing.only(WindowInsetsSides.Start))
            } else {
                Modifier
            }
            Box(Modifier.weight(1f).fillMaxHeight().then(paneModifier).testTag(TranslationPaneTestTags.PANE)) {
                TranslationPaneHost(pane.navController, firstWordHasBack)
            }
        }
    }
}

@Composable
private fun TranslationPaneHost(navController: NavHostController, firstWordHasBack: State<Boolean>) {
    NavHost(navController = navController, startDestination = TranslationPlaceholderRoute) {
        composable<TranslationPlaceholderRoute> { TranslationPlaceholder() }
        translationDestination(navController, firstWordHasBack)
        stressDestinations(navController)
    }
}

@Composable
private fun TranslationPlaceholder() {
    Surface(Modifier.fillMaxSize()) {
        Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
            Text(
                text = stringResource(R.string.translation_select_word),
                modifier = Modifier.testTag(TranslationPaneTestTags.PLACEHOLDER),
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
