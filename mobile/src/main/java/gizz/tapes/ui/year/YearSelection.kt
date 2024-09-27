package gizz.tapes.ui.year

import androidx.annotation.OptIn
import androidx.compose.foundation.layout.RowScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.media3.common.util.UnstableApi
import gizz.tapes.ui.components.CastButton
import gizz.tapes.ui.components.SelectionData
import gizz.tapes.ui.components.SelectionScreen
import gizz.tapes.ui.player.PlayerState
import gizz.tapes.ui.player.PlayerViewModel
import gizz.tapes.util.LCE
import gizz.tapes.util.mapCollection

@OptIn(UnstableApi::class)
@Composable
fun YearSelectionScreen(
    viewModel: YearSelectionViewModel = hiltViewModel(),
    playerViewModel: PlayerViewModel = hiltViewModel(),
    onYearClicked: (year: String) -> Unit,
    onMiniPlayerClick: (title: String) -> Unit,
) {
    val state: LCE<List<YearRenderModel>, Throwable> by viewModel.years.collectAsState()
    val playerState by playerViewModel.playerState.collectAsState()

    YearSelectionScreen(
        yearData = state,
        onYearClicked = onYearClicked,
        onMiniPlayerClick = onMiniPlayerClick,
        playerState = playerState,
        onPauseAction = playerViewModel::pause,
        onPlayAction = playerViewModel::play,
        actions = { CastButton() }
    )
}

@Composable
fun YearSelectionScreen(
    yearData: LCE<List<YearRenderModel>, Throwable>,
    onYearClicked: (year: String) -> Unit,
    onMiniPlayerClick: (title: String) -> Unit,
    playerState: PlayerState,
    onPauseAction: () -> Unit,
    onPlayAction: () -> Unit,
    actions: @Composable RowScope.() -> Unit,
) {
    val selectionData = yearData.mapCollection {
        SelectionData(title = it.year, subtitle = "${it.showCount} shows") {
            onYearClicked(it.year)
        }
    }

    SelectionScreen(
        state = selectionData,
        upClick = null,
        onMiniPlayerClick = onMiniPlayerClick,
        playerState = playerState,
        onPauseAction = onPauseAction,
        onPlayAction = onPlayAction,
        actions = actions
    )
}
