package gizz.tapes.ui

import androidx.annotation.OptIn
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.EaseIn
import androidx.compose.animation.core.EaseOut
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.media3.common.util.UnstableApi
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import gizz.tapes.data.Title
import gizz.tapes.ui.player.FullPlayer
import gizz.tapes.ui.show.ShowScreen
import gizz.tapes.ui.show.ShowSelectionScreen
import gizz.tapes.ui.year.YearSelectionScreen

@UnstableApi
@ExperimentalMaterial3Api
@Composable
fun GizzNavController(
    navController: NavHostController
) {
    NavHost(
        navController = navController,
        startDestination = Screen.YearSelection.route
    ) {
        val miniPlayerClicked = { title: Title ->
            navController.navigate(
                Screen.Player.createRoute(
                    title
                )
            )
        }

        composable(route = Screen.YearSelection.route) {
            YearSelectionScreen(
                onMiniPlayerClick = miniPlayerClicked,
                onYearClicked = { navController.navigate(Screen.ShowSelection.createRoute(it)) }
            )
        }
        composable(
            route = Screen.ShowSelection.route,
            arguments = Screen.ShowSelection.navArguments
        ) {
            ShowSelectionScreen(
                navigateUpClick = { navController.navigateUp() },
                onShowClicked = { id, title ->
                    navController.navigate(
                        Screen.Show.createRoute(
                            showId = id,
                            title = title
                        )
                    )
                },
                onMiniPlayerClick = miniPlayerClicked
            )
        }
        composable(
            route = Screen.Show.route,
            arguments = Screen.Show.navArguments,
        ) {
            ShowScreen(
                upClick = { navController.navigateUp() },
                onMiniPlayerClick = miniPlayerClicked
            )
        }

        fullPlayerNavigation(navController)
    }
}

@OptIn(UnstableApi::class)
fun NavGraphBuilder.fullPlayerNavigation(navController: NavHostController) {
    composable(
        route = Screen.Player.route,
        arguments = Screen.Player.navArguments,
        enterTransition = {
            slideIntoContainer(
                animationSpec = tween(300, easing = EaseIn),
                towards = AnimatedContentTransitionScope.SlideDirection.Up
            )
        },
        exitTransition = {
            slideOutOfContainer(
                animationSpec = tween(300, easing = EaseOut),
                towards = AnimatedContentTransitionScope.SlideDirection.Down
            )
        }
    ) {
        FullPlayer(
            navigateToShow = { id, name ->
                navController.clearBackStack(Screen.Player.route)
                navController.navigate(Screen.Show.createRoute(id, name)) {
                    popUpTo(Screen.YearSelection.route)
                }
            },
            upClick = { navController.navigateUp() },
        )
    }
}
