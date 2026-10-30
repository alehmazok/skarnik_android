package by.mazokaleh.skarnik.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import by.mazokaleh.skarnik.feature.home.ui.HomeRoute
import by.mazokaleh.skarnik.feature.home.ui.HomeScreen

@Composable
fun SkarnikNavHost() {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = HomeRoute) {
        composable<HomeRoute> { HomeScreen() }
    }
}
