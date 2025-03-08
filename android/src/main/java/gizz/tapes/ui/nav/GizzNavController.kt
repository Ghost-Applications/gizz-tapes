package gizz.tapes.ui.nav

import androidx.annotation.OptIn
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.EaseIn
import androidx.compose.animation.core.EaseOut
import androidx.compose.animation.core.tween
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.media3.common.util.UnstableApi
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import gizz.tapes.data.FullShowTitle
import gizz.tapes.ui.menu.about.AboutScreen
import gizz.tapes.ui.menu.settings.SettingsScreen
import gizz.tapes.ui.player.FullPlayer
import gizz.tapes.ui.selection.ShowSelectionScreen
import gizz.tapes.ui.show.ShowScreen
import gizz.tapes.ui.year.YearSelectionScreen

@UnstableApi
@ExperimentalMaterial3Api
@Composable
fun GizzNavController(
    navController: NavHostController
) {
    NavHost(
        navController = navController,
        startDestination = YearSelection
    ) {
        val miniPlayerClicked = { title: FullShowTitle ->
            navController.navigate(Player(title))
        }

        val navigateUp = NavigateUp { navController.navigateUp() }

        composable<YearSelection> {
            YearSelectionScreen(
                onMiniPlayerClick = miniPlayerClicked,
                onYearClicked = { navController.navigate(ShowSelection(it)) },
                navigateToAboutPage = { navController.navigate(About) },
                navigateToSettingsPage = { navController.navigate(Settings) }
            )
        }

        composable<ShowSelection>(
            typeMap = ShowSelection.typeMap
        ) {
            ShowSelectionScreen(
                navigateUpClick = navigateUp,
                onShowClicked = { id, title ->
                    navController.navigate(
                        Show(
                            id = id,
                            title = title
                        )
                    )
                },
                onMiniPlayerClick = miniPlayerClicked
            )
        }

        composable<Show>(
            typeMap = Show.typeMap
        ) {
            ShowScreen(
                navigateUp = navigateUp,
                onMiniPlayerClick = miniPlayerClicked
            )
        }

        fullPlayerNavigation(navController)

        composable<About> {
            AboutScreen(navigateUp = navigateUp)
        }

        composable<Settings> {
            SettingsScreen(navigateUp = navigateUp)
        }
    }
}

@OptIn(UnstableApi::class)
fun NavGraphBuilder.fullPlayerNavigation(navController: NavHostController) {
    composable<Player>(
        typeMap = Player.typeMap,
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
                navController.clearBackStack<Player>()
                navController.navigate(Show(id, name)) {
                    popUpTo(YearSelection)
                }
            },
            navigateUp = { navController.navigateUp() },
        )
    }
}
