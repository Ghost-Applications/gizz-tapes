package gizz.tapes.ui.player

import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import dev.zacsweers.metrox.viewmodel.metroViewModel
import gizz.tapes.LocalPlatformActions
import gizz.tapes.data.FullShowTitle
import gizz.tapes.data.ShowId
import gizz.tapes.nav.NavigateUp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FullPlayerScreen(
    viewModel: PlayerViewModel = metroViewModel(),
    navigateUp: NavigateUp,
    navigateToShow: (ShowId, FullShowTitle) -> Unit,
) {
    val playerState by viewModel.playerState.collectAsState()
    FullPlayerScreen(
        playerState = playerState,
        navigateUp = navigateUp,
        navigateToShow = navigateToShow,
        onPlay = viewModel::play,
        onPause = viewModel::pause,
        onSeek = viewModel::seekTo,
        onSkipPrevious = viewModel::skipToPrevious,
        onSkipNext = viewModel::skipToNext,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FullPlayerScreen(
    playerState: PlayerState,
    navigateUp: NavigateUp,
    navigateToShow: (ShowId, FullShowTitle) -> Unit,
    onPlay: () -> Unit,
    onPause: () -> Unit,
    onSeek: (Int, Long) -> Unit,
    onSkipPrevious: () -> Unit,
    onSkipNext: () -> Unit,
) {
    val snackbarHostState = remember { SnackbarHostState() }

    if (playerState is PlayerState.MediaLoaded.Error) {
        LaunchedEffect(playerState.playerError) {
            snackbarHostState.showSnackbar(
                message = playerState.playerError.message,
                duration = SnackbarDuration.Long
            )
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    if (playerState is PlayerState.MediaLoaded) {
                        Text(
                            text = playerState.showTitle.fullShowTitle.value,
                            maxLines = 1,
                            modifier = Modifier.basicMarquee(iterations = Int.MAX_VALUE),
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { navigateUp() }) {
                        Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Back")
                    }
                },
                actions = {
                    if (playerState is PlayerState.MediaLoaded) {
                        TextButton(onClick = { navigateToShow(playerState.showId, playerState.showTitle) }) {
                            Text("Go to show")
                        }
                    }
                }
            )
        }
    ) { padding ->
        when (playerState) {
            PlayerState.NoMedia -> {
                Box(
                    modifier = Modifier.fillMaxSize().padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No media loaded")
                }
            }
            is PlayerState.MediaLoaded -> {
                FullPlayerContent(
                    state = playerState,
                    modifier = Modifier.padding(padding),
                    onPlay = onPlay,
                    onPause = onPause,
                    onSeek = onSeek,
                    onSkipPrevious = onSkipPrevious,
                    onSkipNext = onSkipNext,
                )
            }
        }
    }
}

@Composable
private fun FullPlayerContent(
    state: PlayerState.MediaLoaded,
    modifier: Modifier = Modifier,
    onPlay: () -> Unit,
    onPause: () -> Unit,
    onSeek: (Int, Long) -> Unit,
    onSkipPrevious: () -> Unit,
    onSkipNext: () -> Unit,
) {
    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        if (maxWidth > maxHeight) {
            LandscapePlayerContent(state, onPlay, onPause, onSeek, onSkipPrevious, onSkipNext)
        } else {
            PortraitPlayerContent(state, onPlay, onPause, onSeek, onSkipPrevious, onSkipNext)
        }
    }
}

@Composable
private fun PortraitPlayerContent(
    state: PlayerState.MediaLoaded,
    onPlay: () -> Unit,
    onPause: () -> Unit,
    onSeek: (Int, Long) -> Unit,
    onSkipPrevious: () -> Unit,
    onSkipNext: () -> Unit,
) {
    val platformActions = LocalPlatformActions.current
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        AsyncImage(
            model = state.artworkUri,
            contentDescription = "Album artwork",
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxWidth(0.8f)
                .aspectRatio(1f)
        )

        Spacer(Modifier.height(24.dp))

        Text(
            text = state.title,
            style = MaterialTheme.typography.titleMedium,
            maxLines = 1,
            modifier = Modifier.basicMarquee(iterations = Int.MAX_VALUE),
        )
        Text(
            text = state.albumTitle,
            style = MaterialTheme.typography.bodyMedium,
            maxLines = 1,
            modifier = Modifier.basicMarquee(iterations = Int.MAX_VALUE),
        )

        Spacer(Modifier.height(16.dp))

        Slider(
            value = state.durationInfo.currentPositionFloat,
            onValueChange = { fraction ->
                val duration = state.durationInfo.duration
                if (duration > 0) {
                    onSeek(state.currentTrackIndex, (fraction * duration).toLong())
                }
            },
            modifier = Modifier.fillMaxWidth()
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(state.durationInfo.elapsedTimeString, style = MaterialTheme.typography.labelSmall)
            Text(state.durationInfo.durationTimeString, style = MaterialTheme.typography.labelSmall)
        }

        Spacer(Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onSkipPrevious, modifier = Modifier.size(48.dp)) {
                Icon(Icons.Default.SkipPrevious, contentDescription = "Previous", modifier = Modifier.size(36.dp))
            }

            if (state is PlayerState.MediaLoaded.Loading) {
                CircularProgressIndicator(modifier = Modifier.size(64.dp))
            } else {
                IconButton(
                    onClick = if (state.isPlaying) onPause else onPlay,
                    modifier = Modifier.size(64.dp)
                ) {
                    Icon(
                        imageVector = if (state.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = if (state.isPlaying) "Pause" else "Play",
                        modifier = Modifier.size(48.dp)
                    )
                }
            }

            IconButton(onClick = onSkipNext, modifier = Modifier.size(48.dp)) {
                Icon(Icons.Default.SkipNext, contentDescription = "Next", modifier = Modifier.size(36.dp))
            }

            platformActions()
        }
    }
}

@Composable
private fun LandscapePlayerContent(
    state: PlayerState.MediaLoaded,
    onPlay: () -> Unit,
    onPause: () -> Unit,
    onSeek: (Int, Long) -> Unit,
    onSkipPrevious: () -> Unit,
    onSkipNext: () -> Unit,
) {
    val platformActions = LocalPlatformActions.current
    Row(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AsyncImage(
            model = state.artworkUri,
            contentDescription = "Album artwork",
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxHeight()
                .aspectRatio(1f)
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .padding(start = 24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = state.title,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                modifier = Modifier
                    .fillMaxWidth()
                    .basicMarquee(iterations = Int.MAX_VALUE),
            )
            Text(
                text = state.albumTitle,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                modifier = Modifier
                    .fillMaxWidth()
                    .basicMarquee(iterations = Int.MAX_VALUE),
            )

            Spacer(Modifier.height(12.dp))

            Slider(
                value = state.durationInfo.currentPositionFloat,
                onValueChange = { fraction ->
                    val duration = state.durationInfo.duration
                    if (duration > 0) {
                        onSeek(state.currentTrackIndex, (fraction * duration).toLong())
                    }
                },
                modifier = Modifier.fillMaxWidth()
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(state.durationInfo.elapsedTimeString, style = MaterialTheme.typography.labelSmall)
                Text(state.durationInfo.durationTimeString, style = MaterialTheme.typography.labelSmall)
            }

            Spacer(Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onSkipPrevious, modifier = Modifier.size(48.dp)) {
                    Icon(Icons.Default.SkipPrevious, contentDescription = "Previous", modifier = Modifier.size(36.dp))
                }

                if (state is PlayerState.MediaLoaded.Loading) {
                    CircularProgressIndicator(modifier = Modifier.size(56.dp))
                } else {
                    IconButton(
                        onClick = if (state.isPlaying) onPause else onPlay,
                        modifier = Modifier.size(56.dp)
                    ) {
                        Icon(
                            imageVector = if (state.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = if (state.isPlaying) "Pause" else "Play",
                            modifier = Modifier.size(40.dp)
                        )
                    }
                }

                IconButton(onClick = onSkipNext, modifier = Modifier.size(48.dp)) {
                    Icon(Icons.Default.SkipNext, contentDescription = "Next", modifier = Modifier.size(36.dp))
                }

                platformActions()
            }
        }
    }
}
