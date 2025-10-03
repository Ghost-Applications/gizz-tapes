package gizz.tapes.nav

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import gizz.tapes.AppGraph
import gizz.tapes.ui.years.YearSelectionScreen

@Composable
fun GizzTapesNavController(appGraph: AppGraph, navController: NavHostController) {
    NavHost(
        navController = navController,
        startDestination = Destination.YearSelection
    ) {
        composable<Destination.YearSelection> {
            YearSelectionScreen(appGraph)
        }
    }
}
