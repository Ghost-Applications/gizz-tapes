package gizz.tapes.ui.venue

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.zacsweers.metrox.viewmodel.assistedMetroViewModel
import gizz.tapes.GizzTapesTheme
import gizz.tapes.LocalPlatformActions
import gizz.tapes.data.Title
import gizz.tapes.nav.NavigateUp
import gizz.tapes.ui.components.ErrorScreen
import gizz.tapes.ui.components.GridItemCard
import gizz.tapes.ui.components.TopAppBarText
import gizz.tapes.ui.components.loadingCards
import gizz.tapes.ui.components.navigationUpIcon
import gizz.tapes.util.LCE

fun interface VenueClicked {
    operator fun invoke(venueName: String)
}

@Composable
fun VenueSelectionScreen(
    navigateUp: NavigateUp,
    onVenueClicked: VenueClicked,
    viewModel: VenueSelectionViewModel = assistedMetroViewModel()
) {
    val state by viewModel.venuesState.collectAsState()

    VenueSelectionScreen(
        countryName = viewModel.countryName,
        navigateUp = navigateUp,
        onVenueClicked = onVenueClicked,
        state = state
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VenueSelectionScreen(
    countryName: String,
    navigateUp: NavigateUp,
    onVenueClicked: VenueClicked,
    state: LCE<List<VenueSelectionData>, Exception>
) {
    val gridState = rememberLazyGridState()
    val snackbarHostState = remember { SnackbarHostState() }

    val platformActions = LocalPlatformActions.current

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    TopAppBarText(
                        title = Title(countryName),
                    )
                },
                navigationIcon = navigationUpIcon(navigateUp),
                actions = {
                    platformActions()
                }
            )
        }
    ) { innerPadding ->
        LazyVerticalGrid(
            state = gridState,
            columns = GridCells.Adaptive(minSize = GizzTapesTheme.size.gridCellMinSize),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxSize().padding(innerPadding)
        ) {
            when (state) {
                is LCE.Content<List<VenueSelectionData>> -> content(state.value, onVenueClicked)
                is LCE.Error<Exception> -> item(span = { GridItemSpan(maxLineSpan) }) {
                    ErrorScreen(state.error)
                }

                LCE.Loading -> loadingCards()
            }
        }
    }
}

private fun LazyGridScope.content(
    countries: List<VenueSelectionData>,
    onVenueClicked: VenueClicked,
) {
    items(countries) { item ->
        GridItemCard(
            title = item.title,
            subtitle = item.subtitle,
            posterUrl = item.posterUrl,
            onClick = { onVenueClicked(item.title.value) }
        )
    }
}
