package by.mazokaleh.skarnik.feature.home.ui

import androidx.activity.compose.BackHandler
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.SaveableStateHolder
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import by.mazokaleh.skarnik.R
import by.mazokaleh.skarnik.core.model.SavedWord
import by.mazokaleh.skarnik.core.ui.WindowWidthClass
import by.mazokaleh.skarnik.feature.favorites.ui.BookmarksScreen
import by.mazokaleh.skarnik.feature.history.ui.HistoryScreen
import by.mazokaleh.skarnik.feature.translation.ui.TranslationRoute
import by.mazokaleh.skarnik.feature.vocabulary.ui.VocabularyScreen
import by.mazokaleh.skarnik.navigation.ListDetailLayout
import by.mazokaleh.skarnik.navigation.rememberTranslationPaneState
import kotlinx.serialization.Serializable

@Serializable
data object HomeRoute

object HomeTestTags {
    const val NAVIGATION_BAR = "home_navigation_bar"
    const val NAVIGATION_RAIL = "home_navigation_rail"
}

/** Bottom navigation destinations. */
enum class HomeTab(
    @StringRes val label: Int,
    @DrawableRes val icon: Int,
    @DrawableRes val selectedIcon: Int,
    val testTag: String
) {
    History(R.string.tab_search, R.drawable.ic_search, R.drawable.ic_search, "home_tab_history"),
    Bookmarks(R.string.tab_bookmarks, R.drawable.ic_bookmark_border, R.drawable.ic_bookmark, "home_tab_bookmarks"),
    Vocabulary(R.string.tab_dictionary, R.drawable.ic_book_outlined, R.drawable.ic_book, "home_tab_dictionary")
}

/**
 * The top-level screen with the navigation bar, or a navigation rail from 600 dp. Each tab keeps its scroll
 * position, ViewModel and the word open beside it while another tab shows; back on another tab returns to History
 * first, as M3 navigation asks. [onOpenTranslation] opens a word full screen, below 840 dp.
 */
@Composable
fun HomeScreen(
    onSearchClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onOpenTranslation: (TranslationRoute) -> Unit
) {
    var tab by rememberSaveable { mutableStateOf(HomeTab.History) }
    val tabStates = rememberSaveableStateHolder()
    BackHandler(enabled = tab != HomeTab.History) { tab = HomeTab.History }

    BoxWithConstraints {
        val widthClass = WindowWidthClass.of(maxWidth)
        val hasRail = widthClass.hasNavigationRail
        Scaffold(
            bottomBar = { if (!hasRail) HomeNavigationBar(tab, onSelect = { tab = it }) },
            // Each tab's own Scaffold handles the status bar; the navigation bar takes the bottom inset.
            contentWindowInsets = WindowInsets(0)
        ) { padding ->
            Row(Modifier.padding(padding).consumeWindowInsets(padding)) {
                if (hasRail) HomeNavigationRail(tab, onSelect = { tab = it })
                // The rail takes the start inset; with it, each tab's Scaffold takes the bottom one.
                val railInsets = WindowInsets.safeDrawing.only(WindowInsetsSides.Start)
                Box(Modifier.weight(1f).then(if (hasRail) Modifier.consumeWindowInsets(railInsets) else Modifier)) {
                    HomeTabContent(tab, tabStates, widthClass, onSearchClick, onSettingsClick, onOpenTranslation)
                }
            }
        }
    }
}

@Composable
private fun HomeTabContent(
    tab: HomeTab,
    tabStates: SaveableStateHolder,
    widthClass: WindowWidthClass,
    onSearchClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onOpenTranslation: (TranslationRoute) -> Unit
) {
    tabStates.SaveableStateProvider(tab) {
        ListDetailLayout(widthClass, rememberTranslationPaneState(), onOpenTranslation) { open, openWord ->
            when (tab) {
                // Opened from history or bookmarks: not saved to history again, as in Flutter.
                HomeTab.History -> HistoryScreen(
                    onSearchClick = onSearchClick,
                    onSettingsClick = onSettingsClick,
                    onWordClick = { open(it.toTranslationRoute()) },
                    openWord = openWord
                )
                HomeTab.Bookmarks -> BookmarksScreen(
                    onWordClick = { open(it.toTranslationRoute()) },
                    openWord = openWord
                )
                // Saved to history, as in Flutter.
                HomeTab.Vocabulary -> VocabularyScreen(
                    onWordClick = { dictionary, word ->
                        open(TranslationRoute(dictionary.langId, word.wordId, word.word))
                    },
                    openWord = openWord
                )
            }
        }
    }
}

private fun SavedWord.toTranslationRoute() =
    TranslationRoute(dictionary.langId, wordId, word, saveToHistory = false)

@Composable
private fun HomeNavigationBar(selected: HomeTab, onSelect: (HomeTab) -> Unit) {
    NavigationBar(Modifier.testTag(HomeTestTags.NAVIGATION_BAR)) {
        HomeTab.entries.forEach { tab ->
            val isSelected = tab == selected
            NavigationBarItem(
                selected = isSelected,
                onClick = { onSelect(tab) },
                icon = { TabIcon(tab, isSelected) },
                label = { Text(stringResource(tab.label)) },
                modifier = Modifier.testTag(tab.testTag)
            )
        }
    }
}

/** M3: the rail replaces the bottom bar on medium and expanded widths (Flutter: the same at 600 dp). */
@Composable
private fun HomeNavigationRail(selected: HomeTab, onSelect: (HomeTab) -> Unit) {
    NavigationRail(Modifier.fillMaxHeight().testTag(HomeTestTags.NAVIGATION_RAIL)) {
        HomeTab.entries.forEach { tab ->
            val isSelected = tab == selected
            NavigationRailItem(
                selected = isSelected,
                onClick = { onSelect(tab) },
                icon = { TabIcon(tab, isSelected) },
                label = { Text(stringResource(tab.label)) },
                modifier = Modifier.testTag(tab.testTag)
            )
        }
    }
}

@Composable
private fun TabIcon(tab: HomeTab, selected: Boolean) {
    Icon(painterResource(if (selected) tab.selectedIcon else tab.icon), contentDescription = null)
}
