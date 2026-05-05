package gizz.tapes.ui

import gizz.tapes.data.FullShowTitle
import gizz.tapes.data.ShowId
import gizz.tapes.data.Year

class MainScreenNavigation(
    val navigateToSettingsScreen: () -> Unit,
    val navigateToAboutScreen: () -> Unit,
    val navigateToShow: (showId: ShowId, showTitle: FullShowTitle) -> Unit,
    val navigateToShowsInYear: (year: Year) -> Unit,
    val navigateToCountryVenues: (countryId: Int, countryName: String) -> Unit,
    val navigateToMusicPlayer: (title: FullShowTitle) -> Unit,
)
