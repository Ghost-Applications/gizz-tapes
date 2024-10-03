package gizz.tapes.ui.year

import androidx.annotation.OptIn
import androidx.compose.foundation.layout.RowScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.media3.common.util.UnstableApi
import gizz.tapes.ui.data.Title
import gizz.tapes.ui.data.Year
import gizz.tapes.ui.components.CastButton
import gizz.tapes.ui.components.SelectionData
import gizz.tapes.ui.components.SelectionScreen
import gizz.tapes.ui.data.Subtitle
import gizz.tapes.ui.player.PlayerState
import gizz.tapes.ui.player.PlayerViewModel
import gizz.tapes.util.LCE
import gizz.tapes.util.mapCollection

@OptIn(UnstableApi::class)
@Composable
fun YearSelectionScreen(
    viewModel: YearSelectionViewModel = hiltViewModel(),
    playerViewModel: PlayerViewModel = hiltViewModel(),
    onYearClicked: (year: Year) -> Unit,
    onMiniPlayerClick: (Title) -> Unit,
) {
    val state: LCE<List<YearSelectionData>, Throwable> by viewModel.years.collectAsState()
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
    yearData: LCE<List<YearSelectionData>, Throwable>,
    onYearClicked: (year: Year) -> Unit,
    onMiniPlayerClick: (title: Title) -> Unit,
    playerState: PlayerState,
    onPauseAction: () -> Unit,
    onPlayAction: () -> Unit,
    actions: @Composable RowScope.() -> Unit,
) {
    val selectionData = yearData.mapCollection {
        SelectionData(
            title = Title(it.year.value),
            subtitle = Subtitle("${it.showCount} shows"),
            posterUrl = it.randomShowPoster,
        ) {
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
