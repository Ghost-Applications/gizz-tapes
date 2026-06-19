package gizz.tapes.ui.downloads

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import dev.zacsweers.metrox.viewmodel.metroViewModel
import gizz.tapes.data.FullShowTitle
import gizz.tapes.data.PosterUrl
import gizz.tapes.data.ShowId
import gizz.tapes.data.Subtitle
import gizz.tapes.data.Title
import gizz.tapes.nav.NavigateUp
import gizz.tapes.ui.components.SelectionData
import gizz.tapes.ui.components.SelectionScreen
import gizz.tapes.ui.player.PlayerActions
import gizz.tapes.ui.player.PlayerViewModel
import gizz.tapes.util.LCE
import gizz.tapes.util.contentOrNull
import gizz.tapes.util.flatMap
import gizz.tapes.util.mapCollection

@Composable
internal fun DownloadedShowsScreen(
    viewModel: DownloadedShowsViewModel = metroViewModel(),
    playerViewModel: PlayerViewModel = metroViewModel(),
    navigateUp: NavigateUp,
    onShowClicked: (ShowId, FullShowTitle) -> Unit,
    onMiniPlayerClick: (FullShowTitle) -> Unit,
) {
    val playerState by playerViewModel.playerState.collectAsState()
    val state by viewModel.shows.collectAsState()

    val selectionData = remember(state) {
        if (state is LCE.Content<List<*>> && state.contentOrNull()?.isEmpty() == true) {
            state.flatMap {
                LCE.Content(listOf(
                    SelectionData(
                        title = Title("No Shows Downlaoded"),
                        subtitle = Subtitle(""),
                        posterUrl = PosterUrl(null),
                        onClick = {}
                    )
                ))
            }
        } else {
            state.mapCollection {
                SelectionData(
                    title = it.showTitle,
                    subtitle = it.showSubTitle,
                    posterUrl = it.posterUrl,
                ) { onShowClicked(it.showId, it.fullShowTitle) }
            }
        }
    }

    SelectionScreen(
        title = Title("Downloaded Shows"),
        state = selectionData,
        playerState = playerState,
        navigateUp = navigateUp,
        onMiniPlayerClick = onMiniPlayerClick,
        playerActions = PlayerActions(
            pause = playerViewModel::pause,
            play = playerViewModel::play,
        ),
        actions = {},
        onViewDownloadsClicked = {}
    )
}
