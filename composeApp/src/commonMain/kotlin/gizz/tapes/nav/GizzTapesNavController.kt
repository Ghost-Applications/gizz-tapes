package gizz.tapes.nav

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.EaseIn
import androidx.compose.animation.core.EaseOut
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import gizz.tapes.nav.Destination.ShowSelection.SelectionType
import gizz.tapes.nav.Destination.ShowSelection.SelectionType.ByYear
import gizz.tapes.ui.MainScreen
import gizz.tapes.ui.MainScreenNavigation
import gizz.tapes.ui.about.AboutScreen
import gizz.tapes.ui.player.FullPlayerScreen
import gizz.tapes.ui.selection.ShowSelectionScreen
import gizz.tapes.ui.settings.SettingsScreen
import gizz.tapes.ui.show.ShowScreen
import gizz.tapes.ui.venue.VenueSelectionScreen

@Composable
fun GizzTapesNavController(navController: NavHostController) {
    NavHost(
        navController = navController,
        startDestination = Destination.Main
    ) {
        composable<Destination.Main> {
            MainScreen(
                navigation = MainScreenNavigation(
                    navigateToSettingsScreen = { navController.navigate(Destination.Settings) },
                    navigateToAboutScreen = { navController.navigate(Destination.About) },
                    navigateToShow = { id, title ->
                        navController.navigate(
                            Destination.Show(
                                id,
                                title
                            )
                        )
                    },
                    navigateToShowsInYear = {
                        navController.navigate(
                            Destination.ShowSelection(ByYear(it))
                        )
                    },
                    navigateToCountryVenues = { id, name ->
                        navController.navigate(
                            Destination.CountryVenues(
                                countryId = id,
                                countryName = name
                            )
                        )
                    },
                    navigateToMusicPlayer = {
                        navController.navigate(Destination.Player(it))
                    }
                )
            )
        }

        composable<Destination.ShowSelection>(
            typeMap = Destination.ShowSelection.typeMap
        ) {
            ShowSelectionScreen(
                navigateUp = { navController.navigateUp() },
                onShowClicked = { id, title ->
                    navController.navigate(Destination.Show(id, title))
                },
                onMiniPlayerClick = { title ->
                    navController.navigate(Destination.Player(title))
                },
            )
        }

        composable<Destination.Show>(
            typeMap = Destination.Show.typeMap
        ) {
            ShowScreen(
                navigateUp = { navController.navigateUp() },
                onMiniPlayerClick = { title ->
                    navController.navigate(Destination.Player(title))
                },
                onPlayerClick = { title ->
                    navController.navigate(Destination.Player(title))
                },
            )
        }

        fullPlayerNavigation(navController)

        composable<Destination.About> {
            AboutScreen(navigateUp = { navController.navigateUp() })
        }

        composable<Destination.Settings> {
            SettingsScreen(navigateUp = { navController.navigateUp() })
        }

        composable<Destination.CountryVenues> {
            VenueSelectionScreen(
                navigateUp = { navController.navigateUp() },
                onVenueClicked = {
                    navController.navigate(Destination.ShowSelection(SelectionType.ByVenue(it)))
                }
            )
        }
    }
}

private fun NavGraphBuilder.fullPlayerNavigation(navController: NavHostController) {
    composable<Destination.Player>(
        typeMap = Destination.Player.typeMap,
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
        FullPlayerScreen(
            navigateToShow = { showId, title ->
                navController.clearBackStack<Destination.Player>()
                navController.navigate(Destination.Show(showId, title)) {
                    popUpTo(Destination.Main)
                }
            },
            navigateUp = { navController.navigateUp() },
        )
    }
}
