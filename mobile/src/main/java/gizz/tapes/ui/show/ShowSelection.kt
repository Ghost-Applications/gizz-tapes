package gizz.tapes.ui.show

import androidx.compose.foundation.layout.RowScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.media3.common.util.UnstableApi
import gizz.tapes.api.data.Show
import gizz.tapes.api.data.ShowsData
import gizz.tapes.ui.components.CastButton
import gizz.tapes.ui.components.SelectionData
import gizz.tapes.ui.components.SelectionScreen
import gizz.tapes.ui.player.PlayerState
import gizz.tapes.ui.player.PlayerViewModel
import gizz.tapes.util.LCE
import gizz.tapes.util.mapCollection
import gizz.tapes.util.showTitle
import gizz.tapes.util.toSimpleFormat

@UnstableApi
@Composable
fun ShowSelectionScreen(
    viewModel: ShowSelectionViewModel = hiltViewModel(),
    playerViewModel: PlayerViewModel = hiltViewModel(),
    navigateUpClick: () -> Unit,
    onShowClicked: (showId: Long, venue: String) -> Unit,
    onMiniPlayerClick: (title: String) -> Unit,
) {
    val playerState by playerViewModel.playerState.collectAsState()
    val state: LCE<List<ShowsData>, Throwable> by viewModel.shows.collectAsState()

    ShowSelectionScreen(
        screenTitle = viewModel.showYear,
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
    screenTitle: String,
    state: LCE<List<ShowsData>, Throwable>,
    playerState: PlayerState,
    navigateUpClick: () -> Unit,
    onShowClicked: (showId: Long, venue: String) -> Unit,
    onMiniPlayerClick: (title: String) -> Unit,
    onPauseAction: () -> Unit,
    onPlayAction: () -> Unit,
    actions: @Composable RowScope.() -> Unit,
) {
    val selectionData = state.mapCollection {
        SelectionData(
            title = it.title,
            subtitle = it.date.toSimpleFormat()
        ) {
//            onShowClicked(it.id, it.showTitle)
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

