@file:OptIn(ExperimentalMaterial3Api::class)

package gizz.tapes.ui.player

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.media3.common.util.UnstableApi
import coil.compose.AsyncImage
import gizz.tapes.R
import gizz.tapes.data.FullShowTitle
import gizz.tapes.data.ShowId
import gizz.tapes.ui.components.CastButton
import gizz.tapes.ui.components.LoadingScreen
import gizz.tapes.ui.components.TopAppBarText
import gizz.tapes.ui.nav.NavigateUp
import gizz.tapes.util.formatedElapsedTime
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlin.math.max

@UnstableApi
@Composable
fun FullPlayer(
    viewModel: PlayerViewModel = hiltViewModel(),
    navigateToShow: (ShowId, FullShowTitle) -> Unit,
    navigateUp: NavigateUp
) {
    val playerState by viewModel.playerState.collectAsState()

    FullPlayer(
        playerState = playerState,
        title = checkNotNull(viewModel.title),
        navigateToShow = navigateToShow,
        navigateUp = navigateUp,
        seekTo = viewModel::seekTo,
        seekToPreviousMediaItem = viewModel::seekToPreviousMediaItem,
        seekToNextMediaItem = viewModel::seekToNextMediaItem,
        onPause = viewModel::pause,
        onPlay = viewModel::play,
        actions = { CastButton() }
    )
}

@UnstableApi
@Composable
fun FullPlayer(
    playerState: PlayerState,
    title: FullShowTitle,
    navigateToShow: (ShowId, FullShowTitle) -> Unit,
    navigateUp: NavigateUp,
    seekTo: (Long) -> Unit,
    seekToPreviousMediaItem: () -> Unit,
    seekToNextMediaItem: () -> Unit,
    onPause: () -> Unit,
    onPlay: () -> Unit,
    actions: @Composable RowScope.() -> Unit = {}
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { TopAppBarText(title.fullShowTitle) },
                navigationIcon = {
                    IconButton(onClick = { navigateUp() }) {
                        Icon(
                            Icons.Default.KeyboardArrowDown,
                            contentDescription = stringResource(R.string.navigate_back)
                        )
                    }
                },
                actions = actions
            )
        }
    ) { innerPadding ->
        when (playerState) {
            is PlayerState.NoMedia -> LoadingScreen()
            is PlayerState.MediaLoaded -> {

                HandlePlayerErrors(playerState, scope, snackbarHostState)

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                ) {
                    Box(modifier = Modifier.fillMaxWidth()) {
                        Button(
                            onClick = {
                                navigateToShow(playerState.showId, playerState.showTitle)
                            },
                            modifier = Modifier.align(Alignment.Center)
                        ) {
                            Text(text = "Go to show")
                        }
                    }

                    AsyncImage(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentScale = ContentScale.Fit,
                        model = playerState.artworkUri,
                        contentDescription = null,
                    )

                    Text(
                        text = playerState.title,
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier
                            .align(Alignment.CenterHorizontally)
                            .basicMarquee(Int.MAX_VALUE)
                            .padding(8.dp)
                    )

                    var sliderValue by remember {
                        mutableFloatStateOf(playerState.durationInfo.currentPositionFloat)
                    }

                    LaunchedEffect(playerState.durationInfo.currentPosition) {
                        sliderValue = playerState.durationInfo.currentPositionFloat
                    }

                    AnimatedContent(
                        targetState = playerState,
                        modifier = Modifier.padding(horizontal = 16.dp),
                        contentKey = { current ->
                            when(current) {
                                is PlayerState.MediaLoaded.Loading -> "Loading"
                                else -> "Media Loaded"
                            }
                        },
                        label = "Full Player Loading State"
                    ) { s ->
                        when (s) {
                            is PlayerState.MediaLoaded.Loading -> LinearProgressIndicator(
                                modifier = Modifier.fillMaxWidth()
                                    .padding(vertical = 8.dp),
                            )

                            else -> Slider(
                                value = sliderValue,
                                onValueChange = {
                                    sliderValue = it
                                },
                                onValueChangeFinished = {
                                    seekTo(sliderValue.toLong())
                                },
                                valueRange = 0f..max(
                                    playerState.durationInfo.duration.toFloat(),
                                    0f
                                )
                            )
                        }
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(IntrinsicSize.Min)
                            .padding(horizontal = 8.dp)
                    ) {
                        Text(
                            text = sliderValue.toLong().formatedElapsedTime,
                            style = MaterialTheme.typography.labelSmall,
                        )

                        Spacer(modifier = Modifier.weight(1f))

                        Text(
                            text = playerState.durationInfo.durationTimeString,
                            style = MaterialTheme.typography.labelSmall
                        )
                    }

                    ActionsRow(
                        seekToPreviousMediaItem,
                        playerState.isPlaying,
                        onPause,
                        onPlay,
                        seekToNextMediaItem
                    )

                    Spacer(modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp))
                }
            }
        }
    }
}

@Composable
private fun HandlePlayerErrors(
    playerState: PlayerState,
    scope: CoroutineScope,
    snackbarHostState: SnackbarHostState
) {
    if (playerState is PlayerState.MediaLoaded.Error) {
        LaunchedEffect(playerState) {
            scope.launch {
                snackbarHostState.showSnackbar(
                    message = playerState.playerError.message,
                    duration = SnackbarDuration.Long
                )
            }
        }
    }
}

@Composable
private fun ActionsRow(
    seekToPreviousMediaItem: () -> Unit,
    playing: Boolean,
    onPause: () -> Unit,
    onPlay: () -> Unit,
    seekToNextMediaItem: () -> Unit
) {
    Row(
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {
        IconButton(onClick = seekToPreviousMediaItem) {
            Icon(
                imageVector = Icons.Default.SkipPrevious,
                contentDescription = "previous song"
            )
        }
        IconButton(
            onClick = {
                if (playing) onPause() else onPlay()
            },
            colors = IconButtonDefaults.iconButtonColors().copy(
                containerColor = MaterialTheme.colorScheme.primaryContainer,
            ),
            modifier = Modifier.size(56.dp)
        ) {
            Icon(
                imageVector = if (!playing) Icons.Default.PlayArrow else Icons.Default.Pause,
                contentDescription = "play"
            )
        }
        IconButton(onClick = seekToNextMediaItem) {
            Icon(
                imageVector = Icons.Default.SkipNext,
                contentDescription = "skip to next song"
            )
        }
    }
}
