package by.mazokaleh.skarnik.feature.home.ui

import androidx.activity.compose.BackHandler
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import by.mazokaleh.skarnik.R
import by.mazokaleh.skarnik.core.model.SavedWord
import by.mazokaleh.skarnik.feature.favorites.ui.BookmarksScreen
import by.mazokaleh.skarnik.feature.history.ui.HistoryScreen
import kotlinx.serialization.Serializable

@Serializable
data object HomeRoute

/** Bottom navigation destinations. The Dictionary tab joins with the vocabulary feature. */
enum class HomeTab(
    @StringRes val label: Int,
    @DrawableRes val icon: Int,
    @DrawableRes val selectedIcon: Int,
    val testTag: String
) {
    History(R.string.tab_search, R.drawable.ic_search, R.drawable.ic_search, "home_tab_history"),
    Bookmarks(R.string.tab_bookmarks, R.drawable.ic_bookmark_border, R.drawable.ic_bookmark, "home_tab_bookmarks")
}

/**
 * The top-level screen with the navigation bar. Each tab keeps its scroll position and ViewModel while another
 * tab shows; back on another tab returns to History first, as M3 navigation asks.
 */
@Composable
fun HomeScreen(onSearchClick: () -> Unit, onWordClick: (SavedWord) -> Unit) {
    var tab by rememberSaveable { mutableStateOf(HomeTab.History) }
    val tabStates = rememberSaveableStateHolder()
    BackHandler(enabled = tab != HomeTab.History) { tab = HomeTab.History }

    Scaffold(
        bottomBar = { HomeNavigationBar(tab, onSelect = { tab = it }) },
        // Each tab's own Scaffold handles the status bar; the navigation bar takes the bottom inset.
        contentWindowInsets = WindowInsets(0)
    ) { padding ->
        Box(Modifier.padding(padding).consumeWindowInsets(padding)) {
            tabStates.SaveableStateProvider(tab) {
                when (tab) {
                    HomeTab.History -> HistoryScreen(onSearchClick = onSearchClick, onWordClick = onWordClick)
                    HomeTab.Bookmarks -> BookmarksScreen(onWordClick = onWordClick)
                }
            }
        }
    }
}

@Composable
private fun HomeNavigationBar(selected: HomeTab, onSelect: (HomeTab) -> Unit) {
    NavigationBar {
        HomeTab.entries.forEach { tab ->
            val isSelected = tab == selected
            NavigationBarItem(
                selected = isSelected,
                onClick = { onSelect(tab) },
                icon = {
                    Icon(painterResource(if (isSelected) tab.selectedIcon else tab.icon), contentDescription = null)
                },
                label = { Text(stringResource(tab.label)) },
                modifier = Modifier.testTag(tab.testTag)
            )
        }
    }
}
