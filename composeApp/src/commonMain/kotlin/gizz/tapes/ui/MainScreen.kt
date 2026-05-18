package gizz.tapes.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.SortByAlpha
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import dev.zacsweers.metrox.viewmodel.assistedMetroViewModel
import dev.zacsweers.metrox.viewmodel.metroViewModel
import gizz.tapes.LocalPlatformActions
import gizz.tapes.data.Title
import gizz.tapes.nav.Destination
import gizz.tapes.ui.components.GizzScaffold
import gizz.tapes.ui.components.TopAppBarText
import gizz.tapes.ui.components.gizzIcon
import gizz.tapes.ui.home.BottomNavBarItem
import gizz.tapes.ui.home.HomeScreen
import gizz.tapes.ui.player.MiniPlayer
import gizz.tapes.ui.player.PlayerActions
import gizz.tapes.ui.player.PlayerViewModel
import gizz.tapes.ui.search.SearchScreen
import gizz.tapes.ui.venue.CountrySelectionScreen
import gizz.tapes.ui.years.YearSelectionScreen
import gizz.tapes.ui.years.YearSelectionViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    navigation: MainScreenNavigation,
) {
    val tabNavController = rememberNavController()

    val gridState = rememberLazyGridState()
    val isScrolled by remember {
        derivedStateOf {
            gridState.firstVisibleItemIndex > 0 || gridState.firstVisibleItemScrollOffset > 200
        }
    }

    val backStack by tabNavController.currentBackStackEntryAsState()
    val currentDest = backStack?.destination
    val isHomeScreen = currentDest?.hasRoute<Destination.Home>() == true
    val isYearScreen = currentDest?.hasRoute<Destination.YearSelection>() == true

    val topBarColor by animateColorAsState(
        targetValue = if (!isHomeScreen || isScrolled) MaterialTheme.colorScheme.surface else Color.Transparent,
        label = "topBarColor"
    )
    val titleAlpha by animateFloatAsState(
        targetValue = if (!isHomeScreen || isScrolled) 1f else 0f,
        label = "titleAlpha"
    )
    val topBarContentColor by animateColorAsState(
        targetValue = if (!isHomeScreen || isScrolled) {
            MaterialTheme.colorScheme.onSurface
        } else {
            MaterialTheme.colorScheme.inverseOnSurface
        },
        label = "topBarContentColor"
    )
    var showMenu by remember { mutableStateOf(false) }

    val playerViewModel = metroViewModel<PlayerViewModel>()
    val playerState by playerViewModel.playerState.collectAsState()

    val yearSelectionViewModel = assistedMetroViewModel<YearSelectionViewModel>()

    val platformActions = LocalPlatformActions.current

    GizzScaffold(
        topAppBar = {
            TopAppBar(
                title = {
                    TopAppBarText(
                        title = Title("Gizz Tapes"),
                        modifier = Modifier.graphicsLayer { alpha = titleAlpha }
                    )
                },
                navigationIcon = gizzIcon(modifier = Modifier.graphicsLayer { alpha = titleAlpha }),
                actions = {
                    platformActions()

                    if (isYearScreen) {
                        IconButton(onClick = { yearSelectionViewModel.toggleSortOrder() }) {
                            Icon(Icons.Default.SortByAlpha, contentDescription = "Sort")
                        }
                    }
                    IconButton(onClick = { showMenu = true }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "More")
                    }
                    DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                        DropdownMenuItem(
                            text = { Text("About") },
                            onClick = {
                                showMenu = false
                                navigation.navigateToAboutScreen()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Settings") },
                            onClick = {
                                showMenu = false
                                navigation.navigateToSettingsScreen()
                            }
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = topBarColor,
                    navigationIconContentColor = topBarContentColor,
                    actionIconContentColor = topBarContentColor,
                    titleContentColor = topBarContentColor,
                )
            )
        },
        bottomBar = {
            NavigationBar {
                BottomNavBarItem.entries.forEach { item ->
                    val selected = currentDest?.hierarchy
                        ?.any { it.hasRoute(item.destination::class) } == true
                    NavigationBarItem(
                        selected = selected,
                        onClick = {
                            tabNavController.navigate(item.destination) {
                                popUpTo(tabNavController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = { Icon(item.icon, contentDescription = item.label) },
                        label = { Text(item.label) }
                    )
                }
            }
        }
    ) { innerPadding, onPlaybackError ->
        Column(
            modifier = Modifier.fillMaxSize()
                .padding(bottom = innerPadding.calculateBottomPadding())
        ) {
            Box(modifier = Modifier.weight(1f)) {
                NavHost(
                    navController = tabNavController,
                    startDestination = Destination.Home,
                ) {
                    composable<Destination.Home> {
                        HomeScreen(
                            gridState = gridState,
                            onShowClicked = { id, title -> navigation.navigateToShow(id, title) }
                        )
                    }
                    composable<Destination.YearSelection> {
                        val yearsState by yearSelectionViewModel.years.collectAsState()
                        YearSelectionScreen(
                            state = yearsState,
                            modifier = Modifier.padding(top = innerPadding.calculateTopPadding()),
                            onYearClicked = { navigation.navigateToShowsInYear(it) }
                        )
                    }
                    composable<Destination.Venues> {
                        CountrySelectionScreen(
                            modifier = Modifier.padding(top = innerPadding.calculateTopPadding()),
                            onCountryClick = { id, name ->
                                navigation.navigateToCountryVenues(id.toInt(), name)
                            }
                        )
                    }
                    composable<Destination.Search> {
                        SearchScreen(
                            modifier = Modifier.padding(top = innerPadding.calculateTopPadding()),
                            onShowClicked = { id, title -> navigation.navigateToShow(id, title) }
                        )
                    }
                }
            }
            MiniPlayer(
                playerState = playerState,
                playerActions = PlayerActions(
                    pause = { playerViewModel.pause() },
                    play = { playerViewModel.play() },
                ),
                playerError = onPlaybackError,
                onClick = navigation.navigateToMusicPlayer::invoke,
            )
        }
    }
}
