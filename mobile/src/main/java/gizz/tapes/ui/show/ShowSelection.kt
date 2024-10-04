package gizz.tapes.ui.show

import androidx.compose.foundation.layout.RowScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.media3.common.util.UnstableApi
import gizz.tapes.ui.components.CastButton
import gizz.tapes.ui.components.SelectionData
import gizz.tapes.ui.components.SelectionScreen
import gizz.tapes.data.ShowId
import gizz.tapes.data.Title
import gizz.tapes.ui.player.PlayerState
import gizz.tapes.ui.player.PlayerViewModel
import gizz.tapes.util.LCE
import gizz.tapes.util.mapCollection

@UnstableApi
@Composable
fun ShowSelectionScreen(
    viewModel: ShowSelectionViewModel = hiltViewModel(),
    playerViewModel: PlayerViewModel = hiltViewModel(),
    navigateUpClick: () -> Unit,
    onShowClicked: (ShowId, Title) -> Unit,
    onMiniPlayerClick: (Title) -> Unit,
) {
    val playerState by playerViewModel.playerState.collectAsState()
    val state: LCE<List<ShowSelectionData>, Throwable> by viewModel.shows.collectAsState()

    ShowSelectionScreen(
        screenTitle = Title(viewModel.showYear),
        state = state,
        playerState = playerState,
        navigateUpClick = navigateUpClick,
        onShowClicked = onShowClicked,
        onMiniPlayerClick = onMiniPlayerClick,
        onPauseAction = playerViewModel::pause,
        onPlayAction = playerViewModel::play,
        actions = { CastButton() }
    )
}

@Composable
fun ShowSelectionScreen(
    screenTitle: Title,
    state: LCE<List<ShowSelectionData>, Throwable>,
    playerState: PlayerState,
    navigateUpClick: () -> Unit,
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
    }

    SelectionScreen(
        title = screenTitle,
        state = selectionData,
        upClick = navigateUpClick,
        onMiniPlayerClick = onMiniPlayerClick,
        playerState = playerState,
        onPauseAction = onPauseAction,
        onPlayAction = onPlayAction,
        actions = actions
    )
}

