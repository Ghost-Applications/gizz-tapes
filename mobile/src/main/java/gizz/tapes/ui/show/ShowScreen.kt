package gizz.tapes.ui.show

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.media3.common.util.UnstableApi
import gizz.tapes.R
import gizz.tapes.api.data.Show
import gizz.tapes.ui.components.NesScaffold
import gizz.tapes.ui.player.PlayerViewModel
import gizz.tapes.ui.theme.GizzTheme
import gizz.tapes.ui.components.CastButton
import gizz.tapes.ui.components.LoadingScreen
import gizz.tapes.ui.player.MiniPlayer
import gizz.tapes.ui.player.PlayerState
import gizz.tapes.ui.player.PlayerState.MediaLoaded
import gizz.tapes.ui.player.PlayerState.NoMedia
import gizz.tapes.util.LCE
import gizz.tapes.util.map

@UnstableApi
@Composable
fun ShowScreen(
    viewModel: ShowViewModel = hiltViewModel(),
    playerViewModel: PlayerViewModel = hiltViewModel(),
    upClick: () -> Unit,
    onMiniPlayerClick: (title: String) -> Unit
) {
    val showState by viewModel.show.collectAsState()
    val appBarTitle by viewModel.appBarTitle.collectAsState()
    val playerState: PlayerState by playerViewModel.playerState.collectAsState()

    var firstLoad by remember { mutableStateOf(true) }

    ShowScreen(
        state = showState,
        playerState = playerState,
        appBarTitle = appBarTitle,
        upClick = upClick,
        onMiniPlayerClick = onMiniPlayerClick,
        onPauseAction = playerViewModel::pause,
        onPlayAction = playerViewModel::play,
        actions = { CastButton() },
        onRowClick = { index, isPlaying ->
            when(val ps = playerState) {
                is NoMedia -> {}
                is MediaLoaded -> {
                    if (!isPlaying) {
                        if (firstLoad) {
                            firstLoad = false
                            showState.map { show ->
                                for (ic in 0 until playerViewModel.mediaItemCount) {
                                    val m = playerViewModel.getMediaItemAt(ic)
//                                    if (m.mediaId == show.tracks.first().mp3) {
//                                        playerViewModel.removeMediaItems(0, ic)
//                                        break
//                                    }
                                }
                            }
                        }

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
    )
}

@Composable
fun ShowScreen(
    state: LCE<Show, Throwable>,
    playerState: PlayerState,
    appBarTitle: String,
    upClick: () -> Unit,
    onRowClick: (index: Int, isPlaying: Boolean) -> Unit,
    onMiniPlayerClick: (title: String) -> Unit,
    onPauseAction: () -> Unit,
    onPlayAction: () -> Unit,
    actions: @Composable RowScope.() -> Unit,
) {
    NesScaffold(
        title = appBarTitle,
        state = state,
        upClick = upClick,
        actions = actions
    ) { value ->
        when (playerState) {
            is NoMedia -> LoadingScreen()
            is MediaLoaded -> ShowListWithPlayer(
                show = value,
                onMiniPlayerClick = onMiniPlayerClick,
                mediaLoaded = playerState,
                onRowClick = onRowClick,
                onPauseAction = onPauseAction,
                onPlayAction = onPlayAction,
                playerState = playerState
            )
        }
    }
}

@Composable
fun ShowListWithPlayer(
    show: Show,
    playerState: PlayerState,
    mediaLoaded: MediaLoaded,
    onRowClick: (index: Int, isPlaying: Boolean) -> Unit,
    onMiniPlayerClick: (title: String) -> Unit,
    onPauseAction: () -> Unit,
    onPlayAction: () -> Unit,
) {
    val currentlyPlayingMediaId = mediaLoaded.mediaId
    val playing = mediaLoaded.isPlaying

    Column {
        LazyColumn(modifier = Modifier
            .fillMaxSize()
            .weight(1f)) {
//            itemsIndexed(show.tracks) { i, track ->
//                val isPlaying = track.mp3 == currentlyPlayingMediaId && playing
//
//                TrackRow(
//                    trackTitle = track.title,
//                    duration = track.formatedDuration,
//                    playing = isPlaying,
//                    onClick = { onRowClick(i, isPlaying) }
//                )
//            }
        }

        MiniPlayer(
            onClick = onMiniPlayerClick,
            playerState = playerState,
            onPauseAction = onPauseAction,
            onPlayAction = onPlayAction
        )
    }
}

@Composable
fun TrackRow(
    trackTitle: String,
    duration: String,
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
        IconButton(
            modifier = Modifier
                .width(80.dp)
                .fillMaxHeight(),
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
                contentDescription = contentDescription
            )
        }

        Column(
            modifier = Modifier.padding(8.dp)
        ) {
            Text(
                text = trackTitle,
                style = MaterialTheme.typography.titleLarge
            )
            Text(
                text = duration,
                style = MaterialTheme.typography.titleMedium
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun TrackRowPreview() {
    GizzTheme {
        TrackRow(
            trackTitle = "The Lizzards",
            duration = "10:00",
            playing = false
        ) {

        }
    }
}
