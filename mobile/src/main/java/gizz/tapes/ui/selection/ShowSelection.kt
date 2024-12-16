package gizz.tapes.ui.selection

import androidx.compose.foundation.layout.RowScope
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SortByAlpha
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.media3.common.util.UnstableApi
import gizz.tapes.data.ShowId
import gizz.tapes.data.SortOrder
import gizz.tapes.data.Title
import gizz.tapes.ui.components.CastButton
import gizz.tapes.ui.components.SelectionData
import gizz.tapes.ui.components.SelectionScreen
import gizz.tapes.ui.nav.NavigateUp
import gizz.tapes.ui.player.PlayerState
import gizz.tapes.ui.player.PlayerViewModel
import gizz.tapes.util.LCE
import gizz.tapes.util.map
import gizz.tapes.util.mapCollection

@UnstableApi
@Composable
fun ShowSelectionScreen(
    viewModel: ShowSelectionViewModel = hiltViewModel(),
    playerViewModel: PlayerViewModel = hiltViewModel(),
    navigateUpClick: NavigateUp,
    onShowClicked: (ShowId, Title) -> Unit,
    onMiniPlayerClick: (Title) -> Unit,
) {
    val playerState by playerViewModel.playerState.collectAsState()
    val state: LCE<List<ShowSelectionData>, Throwable> by viewModel.shows.collectAsState()
    val sortOrder: SortOrder by viewModel.sortOrder.collectAsState()

    ShowSelectionScreen(
        screenTitle = Title(viewModel.showYear),
        state = state,
        playerState = playerState,
        navigateUpClick = navigateUpClick,
        onShowClicked = onShowClicked,
        onMiniPlayerClick = onMiniPlayerClick,
        onPauseAction = playerViewModel::pause,
        onPlayAction = playerViewModel::play,
        sortOrder = sortOrder,
        actions = {
            IconButton(
                onClick = { viewModel.updateSortOrder(!sortOrder) }
            ) {
                Icon(
                    imageVector = Icons.Default.SortByAlpha,
                    contentDescription = "Sort By Date"
                )
            }

            CastButton()
        }
    )
}

@Composable
fun ShowSelectionScreen(
    screenTitle: Title,
    state: LCE<List<ShowSelectionData>, Throwable>,
    sortOrder: SortOrder,
    playerState: PlayerState,
    navigateUpClick: NavigateUp,
    onShowClicked: (ShowId, Title) -> Unit,
    onMiniPlayerClick: (Title) -> Unit,
    onPauseAction: () -> Unit,
    onPlayAction: () -> Unit,
    actions: @Composable RowScope.() -> Unit,
) {
    val selectionData = state.mapCollection {
        SelectionData(
            title = it.showTitle,
            subtitle = it.showSubTitle,
            posterUrl = it.posterUrl,
        ) {
            onShowClicked(it.showId, Title(it.fullShowTitle.toString()))
        }
    }.let { lce ->
        when(sortOrder) {
            SortOrder.Ascending -> lce
            SortOrder.Descending -> lce.map { it.reversed() }
        }
    }

    SelectionScreen(
        title = screenTitle,
        state = selectionData,
        navigateUp = navigateUpClick,
        onMiniPlayerClick = onMiniPlayerClick,
        playerState = playerState,
        onPauseAction = onPauseAction,
        onPlayAction = onPlayAction,
        actions = actions
    )
}

