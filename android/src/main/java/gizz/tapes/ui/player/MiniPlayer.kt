package gizz.tapes.ui.player

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment.Companion.Center
import androidx.compose.ui.Alignment.Companion.CenterVertically
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import gizz.tapes.R
import gizz.tapes.data.FullShowTitle
import gizz.tapes.data.ShowId
import gizz.tapes.data.Title
import gizz.tapes.ui.player.PlayerState.MediaLoaded.Error
import gizz.tapes.ui.player.PlayerState.MediaLoaded.Loading
import kotlinx.datetime.LocalDate

@Composable
fun MiniPlayer(
    playerState: PlayerState,
    onClick: (FullShowTitle) -> Unit,
    onPauseAction: () -> Unit,
    onPlayAction: () -> Unit,
    playerError: (PlayerError) -> Unit,
) {
    when (playerState) {
        is PlayerState.NoMedia -> return
        is PlayerState.MediaLoaded -> {
            val playing = playerState.isPlaying
            val elapsedTime = playerState.durationInfo.elapsedTimeString

            if (playerState is Error) {
                playerError(playerState.playerError)
            }

            Surface(
                shadowElevation = 8.dp
            ) {
                Column {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(64.dp)
                            .shadow(4.dp)
                            .background(MaterialTheme.colorScheme.primaryContainer)
                            .clickable {
                                onClick(playerState.showTitle)
                            },
                    ) {

                        AsyncImage(
                            model = playerState.artworkUri,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            contentScale = ContentScale.Crop
                        )

                        Column(
                            modifier = Modifier
                                .padding(8.dp)
                                .weight(1f)
                        ) {

                            Text(
                                text = playerState.title,
                                style = MaterialTheme.typography.bodyLarge,
                                modifier = Modifier.basicMarquee(
                                    iterations = Int.MAX_VALUE
                                ),
                                maxLines = 1
                            )
                            Text(
                                text = playerState.albumTitle,
                                style = MaterialTheme.typography.bodySmall,
                                maxLines = 1
                            )
                        }

                        Box(modifier = Modifier.fillMaxHeight()) {
                            Text(
                                text = elapsedTime,
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.align(Center)
                            )
                        }

                        IconButton(
                            onClick = {
                                if (playing) {
                                    onPauseAction()
                                } else {
                                    onPlayAction()
                                }
                            }
                        ) {
                            val (imageVector, contentDescription) = if (playing) {
                                Icons.Default.Pause to stringResource(R.string.pause)
                            } else {
                                Icons.Default.PlayArrow to stringResource(R.string.pause)
                            }

                            Icon(
                                imageVector = imageVector,
                                contentDescription = contentDescription,
                                modifier = Modifier.align(CenterVertically)
                            )
                        }
                    }

                    val shouldDisplay = playerState is Loading
                    AnimatedVisibility(
                        visible = shouldDisplay,
                        enter = fadeIn(),
                        exit = fadeOut()
                    ) {
                        LinearProgressIndicator(
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
            }
        }
    }
}

@Composable
@Preview
fun MiniPlayerPreview() {

    val state = Loading(
        showId = ShowId("showId"),
        showTitle = FullShowTitle(title = Title("Show Title"), LocalDate(2024, 10, 1)),
        durationInfo = MediaDurationInfo(currentPosition = 0, duration = 1000),
        artworkUri = null,
        title = "Title",
        albumTitle = "AlbumTitle",
        mediaId = "mediaId",
    )

    Box(
        modifier = Modifier.fillMaxSize()
            .height(64.dp)
    ) {
        MiniPlayer(
            playerState = state,
            onClick = {},
            onPlayAction = {},
            onPauseAction = {},
            playerError = {}
        )
    }
}

