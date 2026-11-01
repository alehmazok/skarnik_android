package by.mazokaleh.skarnik.navigation

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.remember
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.dropUnlessResumed
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import by.mazokaleh.skarnik.core.model.WordLink
import by.mazokaleh.skarnik.core.ui.WindowWidthClass
import by.mazokaleh.skarnik.feature.home.ui.HomeRoute
import by.mazokaleh.skarnik.feature.home.ui.HomeScreen
import by.mazokaleh.skarnik.feature.search.ui.SearchRoute
import by.mazokaleh.skarnik.feature.search.ui.SearchScreen
import by.mazokaleh.skarnik.feature.settings.ui.SettingsRoute
import by.mazokaleh.skarnik.feature.settings.ui.SettingsScreen
import by.mazokaleh.skarnik.feature.stress.ui.StressRoute
import by.mazokaleh.skarnik.feature.stress.ui.stressDestinations
import by.mazokaleh.skarnik.feature.translation.ui.TranslationRoute
import by.mazokaleh.skarnik.feature.translation.ui.TranslationScreen
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow

@Composable
fun SkarnikNavHost(wordLinks: Flow<WordLink> = emptyFlow()) {
    val navController = rememberNavController()
    WordLinkEffect(navController, wordLinks)
    NavHost(navController = navController, startDestination = HomeRoute) {
        composable<HomeRoute> { entry ->
            HomeScreen(
                onSearchClick = dropUnlessResumed { navController.navigate(SearchRoute) },
                onSettingsClick = dropUnlessResumed { navController.navigate(SettingsRoute) },
                // A second tap while the transition runs must not open the word twice.
                onOpenTranslation = { route -> navController.navigateIfResumed(entry, route) }
            )
        }
        searchDestination(navController)
        composable<SettingsRoute> {
            SettingsScreen(onBack = dropUnlessResumed { navController.popBackStack() })
        }
        translationDestination(navController)
        stressDestinations(navController)
    }
}

private fun NavGraphBuilder.searchDestination(navController: NavHostController) {
    composable<SearchRoute> { entry ->
        BoxWithConstraints {
            ListDetailLayout(
                widthClass = WindowWidthClass.of(maxWidth),
                pane = rememberTranslationPaneState(),
                onOpenFullScreen = { route -> navController.navigateIfResumed(entry, route) }
            ) { open, openWord ->
                SearchScreen(
                    onBack = dropUnlessResumed { navController.popBackStack() },
                    onWordClick = { word -> open(TranslationRoute(word.dictionary.langId, word.wordId, word.word)) },
                    openWord = openWord
                )
            }
        }
    }
}

private fun NavHostController.navigateIfResumed(entry: NavBackStackEntry, route: Any) {
    if (entry.lifecycle.currentState == Lifecycle.State.RESUMED) navigate(route)
}

/**
 * The translation screen, full screen or in a [ListDetailLayout] pane. In a pane, [firstWordHasBack] tells whether
 * the word opened from the list gets a back arrow (not while the list shows beside it); words opened from links
 * always have one.
 */
internal fun NavGraphBuilder.translationDestination(
    navController: NavHostController,
    firstWordHasBack: State<Boolean>? = null
) {
    composable<TranslationRoute> { entry ->
        val isFirstInPane = remember(entry) {
            navController.previousBackStackEntry?.destination?.hasRoute<TranslationPlaceholderRoute>() == true
        }
        val hasBack = firstWordHasBack == null || firstWordHasBack.value || !isFirstInPane
        TranslationScreen(
            route = entry.toRoute(),
            onBack = if (hasBack) dropUnlessResumed { navController.popBackStack() } else null,
            onOpenWord = { link ->
                navController.navigateIfResumed(entry, TranslationRoute(link.dictionary.langId, link.wordId))
            },
            onRedirect = { target, fromWord ->
                // The alias has nothing to show: the target replaces it, so back skips it.
                navController.navigate(
                    TranslationRoute(target.dictionary.langId, target.wordId, redirectedFrom = fromWord)
                ) {
                    popUpTo<TranslationRoute> { inclusive = true }
                }
            },
            onStress = { word -> navController.navigateIfResumed(entry, StressRoute(word)) }
        )
    }
}
