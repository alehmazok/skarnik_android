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
import by.mazokaleh.skarnik.feature.translation.ui.TranslationRoute
import by.mazokaleh.skarnik.feature.translation.ui.TranslationScreen

@Composable
fun SkarnikNavHost() {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = HomeRoute) {
        composable<HomeRoute> {
            HomeScreen(onSearchClick = dropUnlessResumed { navController.navigate(SearchRoute) })
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
        composable<TranslationRoute> { entry ->
            TranslationScreen(route = entry.toRoute(), onBack = dropUnlessResumed { navController.popBackStack() })
        }
    }
}
