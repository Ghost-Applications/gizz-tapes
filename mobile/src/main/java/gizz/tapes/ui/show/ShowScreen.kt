package gizz.tapes.ui.show

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.BlendModeColorFilter
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.media3.common.util.UnstableApi
import coil.compose.AsyncImage
import gizz.tapes.R
import gizz.tapes.ui.components.GizzScaffold
import gizz.tapes.ui.player.PlayerViewModel
import gizz.tapes.ui.theme.GizzTheme
import gizz.tapes.ui.components.CastButton
import gizz.tapes.ui.components.ErrorScreen
import gizz.tapes.ui.components.LoadingScreen
import gizz.tapes.data.PosterUrl
import gizz.tapes.data.Title
import gizz.tapes.ui.player.MiniPlayer
import gizz.tapes.ui.player.PlayerError
import gizz.tapes.ui.player.PlayerState
import gizz.tapes.ui.player.PlayerState.MediaLoaded
import gizz.tapes.ui.player.PlayerState.NoMedia
import gizz.tapes.util.LCE
import gizz.tapes.util.contentOrNull
import kotlin.time.Duration.Companion.seconds

@UnstableApi
@Composable
fun ShowScreen(
    viewModel: ShowViewModel = hiltViewModel(),
    playerViewModel: PlayerViewModel = hiltViewModel(),
    upClick: () -> Unit,
    onMiniPlayerClick: (Title) -> Unit
) {
    val showState by viewModel.show.collectAsState()
    val playerState: PlayerState by playerViewModel.playerState.collectAsState()

    var firstLoad by remember { mutableStateOf(true) }

    ShowScreen(
        state = showState,
        playerState = playerState,
        appBarTitle = viewModel.title,
        upClick = upClick,
        onMiniPlayerClick = onMiniPlayerClick,
        onPauseAction = playerViewModel::pause,
        onPlayAction = playerViewModel::play,
        actions = { CastButton() },
        onRowClick = { index, isPlaying ->
            if (firstLoad) {
                firstLoad = false
                val content = checkNotNull(showState.contentOrNull())
                content.removeOldMediaItemsAndAddNew()
                playerViewModel.play()
            } else {
                when (val ps = playerState) {
                    is NoMedia -> {}
                    is MediaLoaded -> {
                        if (!isPlaying) {
                            if (ps.mediaId != playerViewModel.getMediaItemAt(index).mediaId) {
                                playerViewModel.seekTo(index, 0)
                            }
                            playerViewModel.play()
                        } else {
                            playerViewModel.pause()
                        }
                    }
                }
            }
        }
    )
}

@Composable
fun ShowScreen(
    state: LCE<ShowScreenData, Throwable>,
    playerState: PlayerState,
    appBarTitle: Title,
    upClick: () -> Unit,
    onRowClick: (index: Int, isPlaying: Boolean) -> Unit,
    onMiniPlayerClick: (Title) -> Unit,
    onPauseAction: () -> Unit,
    onPlayAction: () -> Unit,
    actions: @Composable RowScope.() -> Unit,
) {
    GizzScaffold(
        title = appBarTitle,
        state = state,
        upClick = upClick,
        actions = actions
    ) { value, playerError ->
        when(state) {
            is LCE.Content -> ShowListWithPlayer(
                showData = value,
                onMiniPlayerClick = onMiniPlayerClick,
                onRowClick = onRowClick,
                onPauseAction = onPauseAction,
                onPlayAction = onPlayAction,
                playerState = playerState,
                playerError = playerError
            )
            is LCE.Error -> ErrorScreen(state.userDisplayedMessage)
            LCE.Loading -> LoadingScreen()
        }
    }
}

@Composable
fun ShowListWithPlayer(
    showData: ShowScreenData,
    playerState: PlayerState,
    onRowClick: (index: Int, isPlaying: Boolean) -> Unit,
    onMiniPlayerClick: (Title) -> Unit,
    onPauseAction: () -> Unit,
    onPlayAction: () -> Unit,
    playerError: (PlayerError) -> Unit,
) {
    val (currentlyPlayingMediaId, playing) = when(playerState) {
        is MediaLoaded -> playerState.mediaId to playerState.isPlaying
        NoMedia -> "" to false
    }

    Column {
        LazyColumn(modifier = Modifier
            .fillMaxSize()
            .weight(1f)) {
            itemsIndexed(showData.tracks) { i, track ->
                val isPlaying = track.id.id == currentlyPlayingMediaId && playing

                TrackRow(
                    trackTitle = track.title,
                    duration = track.duration,
                    posterUrl = showData.showPosterUrl,
                    playing = isPlaying,
                    onClick = { onRowClick(i, isPlaying) }
                )
            }
        }

        MiniPlayer(
            onClick = onMiniPlayerClick,
            playerState = playerState,
            onPauseAction = onPauseAction,
            onPlayAction = onPlayAction,
            playerError = playerError
        )
    }
}

@Composable
fun TrackRow(
    trackTitle: TrackTitle,
    duration: TrackDuration,
    posterUrl: PosterUrl,
    playing: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .padding(4.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .fillMaxWidth()
            .requiredHeight(96.dp)
            .height(IntrinsicSize.Max)
            .clickable {
                onClick()
            }
    ) {
        Box(
            modifier = Modifier
                .width(80.dp)
                .fillMaxHeight()
        ) {
            AsyncImage(
                model = posterUrl.value,
                contentDescription = null,
                contentScale = ContentScale.FillBounds,
                modifier = Modifier.fillMaxSize()
            )

            IconButton(
                modifier = Modifier
                    .align(Alignment.Center),
                onClick = {
                    onClick()
                }
            ) {
                val (imageVector, contentDescription) = if (playing) {
                    Icons.Default.Pause to stringResource(R.string.pause)
                } else {
                    Icons.Default.PlayArrow to stringResource(R.string.play)
                }

                Icon(
                    imageVector = imageVector,
                    contentDescription = contentDescription,
                    tint = Color.White
                )
            }
        }

        Column(
            modifier = Modifier.padding(8.dp)
                .align(Alignment.CenterVertically)
        ) {
            Text(
                text = trackTitle.title,
                style = MaterialTheme.typography.bodyLarge
            )
            Text(
                text = duration.formatedDuration,
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun TrackRowPreview() {
    GizzTheme {
        TrackRow(
            trackTitle = TrackTitle("The Lizzards"),
            duration = TrackDuration((10 * 60).seconds),
            posterUrl = PosterUrl(null),
            playing = false
        ) {

        }
    }
}
