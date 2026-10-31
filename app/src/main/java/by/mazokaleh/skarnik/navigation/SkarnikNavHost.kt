package by.mazokaleh.skarnik.navigation

import androidx.compose.runtime.Composable
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.dropUnlessResumed
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import by.mazokaleh.skarnik.feature.home.ui.HomeRoute
import by.mazokaleh.skarnik.feature.home.ui.HomeScreen
import by.mazokaleh.skarnik.feature.search.ui.SearchRoute
import by.mazokaleh.skarnik.feature.search.ui.SearchScreen
import by.mazokaleh.skarnik.feature.settings.ui.SettingsRoute
import by.mazokaleh.skarnik.feature.settings.ui.SettingsScreen
import by.mazokaleh.skarnik.feature.stress.ui.StressRoute
import by.mazokaleh.skarnik.feature.stress.ui.StressScreen
import by.mazokaleh.skarnik.feature.stress.ui.StressTableRoute
import by.mazokaleh.skarnik.feature.stress.ui.StressTableScreen
import by.mazokaleh.skarnik.feature.translation.ui.TranslationRoute
import by.mazokaleh.skarnik.feature.translation.ui.TranslationScreen

@Composable
fun SkarnikNavHost() {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = HomeRoute) {
        composable<HomeRoute> { entry ->
            HomeScreen(
                onSearchClick = dropUnlessResumed { navController.navigate(SearchRoute) },
                onSettingsClick = dropUnlessResumed { navController.navigate(SettingsRoute) },
                onWordClick = { word ->
                    if (entry.lifecycle.currentState == Lifecycle.State.RESUMED) {
                        // Opened from history or bookmarks: not saved to history again, as in Flutter.
                        navController.navigate(
                            TranslationRoute(word.dictionary.langId, word.wordId, word.word, saveToHistory = false)
                        )
                    }
                },
                onDictionaryWordClick = { dictionary, word ->
                    if (entry.lifecycle.currentState == Lifecycle.State.RESUMED) {
                        // Saved to history, as in Flutter.
                        navController.navigate(TranslationRoute(dictionary.langId, word.wordId, word.word))
                    }
                }
            )
        }
        composable<SearchRoute> { entry ->
            SearchScreen(
                onBack = dropUnlessResumed { navController.popBackStack() },
                onWordClick = { word ->
                    // A second tap while the transition runs must not open the word twice.
                    if (entry.lifecycle.currentState == Lifecycle.State.RESUMED) {
                        navController.navigate(
                            TranslationRoute(langId = word.dictionary.langId, wordId = word.wordId, word = word.word)
                        )
                    }
                }
            )
        }
        composable<SettingsRoute> {
            SettingsScreen(onBack = dropUnlessResumed { navController.popBackStack() })
        }
        composable<TranslationRoute> { entry ->
            TranslationScreen(
                route = entry.toRoute(),
                onBack = dropUnlessResumed { navController.popBackStack() },
                onOpenWord = { link ->
                    if (entry.lifecycle.currentState == Lifecycle.State.RESUMED) {
                        navController.navigate(TranslationRoute(link.dictionary.langId, link.wordId))
                    }
                },
                onRedirect = { target, fromWord ->
                    // The alias has nothing to show: the target replaces it, so back skips it.
                    navController.navigate(
                        TranslationRoute(target.dictionary.langId, target.wordId, redirectedFrom = fromWord)
                    ) {
                        popUpTo<TranslationRoute> { inclusive = true }
                    }
                },
                onStress = { word ->
                    if (entry.lifecycle.currentState == Lifecycle.State.RESUMED) {
                        navController.navigate(StressRoute(word))
                    }
                }
            )
        }
        composable<StressRoute> { entry ->
            StressScreen(
                route = entry.toRoute(),
                onBack = dropUnlessResumed { navController.popBackStack() },
                onOpenTable = { stressEntry, replace ->
                    if (replace) {
                        // The only match: its table replaces the lookup, so back returns to the translation.
                        navController.navigate(StressTableRoute(stressEntry)) {
                            popUpTo<StressRoute> { inclusive = true }
                        }
                    } else if (entry.lifecycle.currentState == Lifecycle.State.RESUMED) {
                        navController.navigate(StressTableRoute(stressEntry))
                    }
                }
            )
        }
        composable<StressTableRoute> { entry ->
            StressTableScreen(
                route = entry.toRoute(),
                onBack = dropUnlessResumed { navController.popBackStack() }
            )
        }
    }
}
