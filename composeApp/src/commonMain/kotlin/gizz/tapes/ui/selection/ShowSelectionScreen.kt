package gizz.tapes.ui.selection

import androidx.compose.foundation.layout.RowScope
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SortByAlpha
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import dev.zacsweers.metrox.viewmodel.assistedMetroViewModel
import dev.zacsweers.metrox.viewmodel.metroViewModel
import gizz.tapes.data.FullShowTitle
import gizz.tapes.data.ShowId
import gizz.tapes.data.ShowSelectionData
import gizz.tapes.data.Title
import gizz.tapes.nav.NavigateUp
import gizz.tapes.ui.components.SelectionData
import gizz.tapes.ui.components.SelectionScreen
import gizz.tapes.ui.player.PlayerActions
import gizz.tapes.ui.player.PlayerState
import gizz.tapes.ui.player.PlayerViewModel
import gizz.tapes.util.LCE
import gizz.tapes.util.mapCollection

@Composable
internal fun ShowSelectionScreen(
    viewModel: ShowSelectionViewModel = assistedMetroViewModel(),
    playerViewModel: PlayerViewModel = metroViewModel(),
    navigateUp: NavigateUp,
    onShowClicked: (ShowId, FullShowTitle) -> Unit,
    onMiniPlayerClick: (FullShowTitle) -> Unit,
) {
    val playerState by playerViewModel.playerState.collectAsState()
    val state by viewModel.shows.collectAsState()

    ShowSelectionScreen(
        screenTitle = Title(viewModel.selectionType.title),
        state = state,
        playerState = playerState,
        navigateUp = navigateUp,
        onShowClicked = onShowClicked,
        onMiniPlayerClick = onMiniPlayerClick,
        playerActions = PlayerActions(
            pause = playerViewModel::pause,
            play = playerViewModel::play,
        ),
        actions = {
            IconButton(onClick = { viewModel.toggleSortOrder() }) {
                Icon(Icons.Default.SortByAlpha, contentDescription = "Sort by date")
            }
        }
    )
}

@Composable
fun ShowSelectionScreen(
    screenTitle: Title,
    state: LCE<List<ShowSelectionData>, Exception>,
    playerState: PlayerState,
    navigateUp: NavigateUp,
    onShowClicked: (ShowId, FullShowTitle) -> Unit,
    onMiniPlayerClick: (FullShowTitle) -> Unit,
    playerActions: PlayerActions,
    actions: @Composable RowScope.() -> Unit,
) {
    val selectionData = remember(state) {
        state.mapCollection {
            SelectionData(
                title = it.showTitle,
                subtitle = it.showSubTitle,
                posterUrl = it.posterUrl,
            ) { onShowClicked(it.showId, it.fullShowTitle) }
        }
    }

    SelectionScreen(
        title = screenTitle,
        state = selectionData,
        navigateUp = navigateUp,
        onMiniPlayerClick = onMiniPlayerClick,
        playerState = playerState,
        playerActions = playerActions,
        actions = actions
    )
}
