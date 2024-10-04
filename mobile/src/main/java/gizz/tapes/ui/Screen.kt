package gizz.tapes.ui

import androidx.navigation.NamedNavArgument
import androidx.navigation.NavType
import androidx.navigation.navArgument
import gizz.tapes.data.ShowId
import gizz.tapes.data.Title
import gizz.tapes.data.Year

sealed class Screen(
    val route: String,
    val navArguments: List<NamedNavArgument> = emptyList()
) {
    data object YearSelection : Screen("yearSelection")

    data object ShowSelection : Screen(
        route = "shows/{year}",
        navArguments = listOf(navArgument("year") { type = NavType.StringType })
    ) {
        fun createRoute(year: Year) = "shows/${year.value}"
    }

    data object Show : Screen(
        route = "show/{id}/{title}",
        navArguments = listOf(
            navArgument("id") { type = NavType.StringType },
            navArgument("title") { type = NavType.StringType }
        )
    ) {
        fun createRoute(showId: ShowId, title: Title) = "show/${showId.value}/${title.encodedTitle}"
    }

    data object Player : Screen(
        route = "player/{title}",
        navArguments = listOf(
            navArgument("title") { type = NavType.StringType }
        )
    ) {
        fun createRoute(title: Title) = "player/${title.encodedTitle}"
    }
}
