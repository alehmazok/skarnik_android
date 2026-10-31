package by.mazokaleh.skarnik.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.navigation.NavHostController
import by.mazokaleh.skarnik.core.model.WordLink
import by.mazokaleh.skarnik.feature.home.ui.HomeRoute
import by.mazokaleh.skarnik.feature.translation.ui.TranslationRoute
import kotlinx.coroutines.flow.Flow

/**
 * Opens each word from [wordLinks] (widget taps, app links) over the start screen: back from the word returns to
 * History, whatever was open before. The word is saved to history, as a Flutter deep link was.
 */
@Composable
fun WordLinkEffect(navController: NavHostController, wordLinks: Flow<WordLink>) {
    LaunchedEffect(navController, wordLinks) {
        wordLinks.collect { link ->
            navController.navigate(TranslationRoute(link.dictionary.langId, link.wordId)) {
                popUpTo<HomeRoute>()
            }
        }
    }
}
