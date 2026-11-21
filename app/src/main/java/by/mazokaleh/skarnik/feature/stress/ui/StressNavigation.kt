package by.mazokaleh.skarnik.feature.stress.ui

import androidx.lifecycle.compose.dropUnlessResumed
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import by.mazokaleh.skarnik.core.analytics.Screen
import by.mazokaleh.skarnik.navigation.TrackScreenView

/**
 * The stress table screen, opened with [StressTableRoute] from a translation's «На́ціск» (the lookup and the
 * homonym picker sheet live on the translation, see [StressLookupViewModel]).
 */
fun NavGraphBuilder.stressDestinations(navController: NavHostController) {
    composable<StressTableRoute> { entry ->
        TrackScreenView(Screen.StressTable)
        StressTableScreen(
            route = entry.toRoute(),
            onBack = dropUnlessResumed { navController.popBackStack() }
        )
    }
}
