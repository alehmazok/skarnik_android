package by.mazokaleh.skarnik.feature.stress.ui

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.dropUnlessResumed
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import androidx.navigation.toRoute

/** The stress lookup and table screens, opened with [StressRoute] from a translation. */
fun NavGraphBuilder.stressDestinations(navController: NavHostController) {
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
