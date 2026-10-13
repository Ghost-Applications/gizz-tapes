package gizz.tapes.ui.saved

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import dev.zacsweers.metrox.viewmodel.metroViewModel
import gizz.tapes.data.DownloadedShowData
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
internal fun RecentShowsScreen(
    viewModel: RecentShowsViewModel = metroViewModel(),
    navigateUp: NavigateUp,
    onShowClicked: (ShowId, FullShowTitle) -> Unit,
    onMiniPlayerClick: (FullShowTitle) -> Unit,
) {
    val state by viewModel.shows.collectAsState()
    SavedShowsScreen(
        title = Title("Recent Shows"),
        emptyMessage = Title("No Recently Played Shows"),
        state = state,
        navigateUp = navigateUp,
        onShowClicked = onShowClicked,
        onMiniPlayerClick = onMiniPlayerClick,
    )
}

@Composable
internal fun FavoriteShowsScreen(
    viewModel: FavoriteShowsViewModel = metroViewModel(),
    navigateUp: NavigateUp,
    onShowClicked: (ShowId, FullShowTitle) -> Unit,
    onMiniPlayerClick: (FullShowTitle) -> Unit,
) {
    val state by viewModel.shows.collectAsState()
    SavedShowsScreen(
        title = Title("Favorite Shows"),
        emptyMessage = Title("No Favorite Shows"),
        state = state,
        navigateUp = navigateUp,
        onShowClicked = onShowClicked,
        onMiniPlayerClick = onMiniPlayerClick,
    )
}

@Composable
private fun SavedShowsScreen(
    title: Title,
    emptyMessage: Title,
    state: LCE<List<DownloadedShowData>, Exception>,
    navigateUp: NavigateUp,
    onShowClicked: (ShowId, FullShowTitle) -> Unit,
    onMiniPlayerClick: (FullShowTitle) -> Unit,
    playerViewModel: PlayerViewModel = metroViewModel(),
) {
    val playerState by playerViewModel.playerState.collectAsState()

    val selectionData = remember(state) {
        if (state is LCE.Content<List<*>> && state.contentOrNull()?.isEmpty() == true) {
            state.flatMap {
                LCE.Content(listOf(
                    SelectionData(
                        title = emptyMessage,
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
        title = title,
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
