@file:OptIn(kotlin.time.ExperimentalTime::class)

package gizz.tapes.ui.show

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withLink
import androidx.compose.ui.unit.dp
import de.charlex.compose.htmltext.material3.HtmlText
import dev.zacsweers.metrox.viewmodel.assistedMetroViewModel
import dev.zacsweers.metrox.viewmodel.metroViewModel
import gizz.tapes.GizzTapesTheme
import gizz.tapes.LocalPlatformActions
import gizz.tapes.data.FullShowTitle
import gizz.tapes.data.RecordingData
import gizz.tapes.data.RecordingId
import gizz.tapes.nav.NavigateUp
import gizz.tapes.ui.components.ErrorScreen
import gizz.tapes.ui.components.GizzScaffold
import gizz.tapes.ui.components.LoadingScreen
import gizz.tapes.ui.components.PosterImage
import gizz.tapes.ui.components.TopAppBarText
import gizz.tapes.ui.components.navigationUpIcon
import gizz.tapes.ui.player.MiniPlayer
import gizz.tapes.ui.player.PlayerActions
import gizz.tapes.ui.player.PlayerState
import gizz.tapes.ui.player.PlayerViewModel
import gizz.tapes.util.LCE
import gizz.tapes.util.toAlbumFormat

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShowScreen(
    viewModel: ShowViewModel = assistedMetroViewModel(),
    playerViewModel: PlayerViewModel = metroViewModel(),
    navigateUp: NavigateUp,
    onMiniPlayerClick: (FullShowTitle) -> Unit,
    onPlayerClick: (FullShowTitle) -> Unit,
) {
    val showState by viewModel.show.collectAsState()
    val playerState by playerViewModel.playerState.collectAsState()

    ShowScreen(
        title = viewModel.title,
        showState = showState,
        playerState = playerState,
        navigateUp = navigateUp,
        onMiniPlayerClick = onMiniPlayerClick,
        onPlayerClick = onPlayerClick,
        onRecordingChange = viewModel::changeSelectedRecording,
        onPauseAction = playerViewModel::pause,
        onPlayAction = playerViewModel::play,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShowScreen(
    title: FullShowTitle,
    showState: LCE<ShowScreenState, Throwable>,
    playerState: PlayerState,
    navigateUp: NavigateUp,
    onMiniPlayerClick: (FullShowTitle) -> Unit,
    onPlayerClick: (FullShowTitle) -> Unit,
    onRecordingChange: (RecordingId) -> Unit,
    onPauseAction: () -> Unit,
    onPlayAction: () -> Unit,
) {
    val platformActions = LocalPlatformActions.current
    val lazyListState = rememberLazyListState()

    val isScrolled by remember {
        derivedStateOf {
            lazyListState.firstVisibleItemIndex > 0 || lazyListState.firstVisibleItemScrollOffset > 200
        }
    }
    val topBarColor by animateColorAsState(
        targetValue = if (isScrolled) MaterialTheme.colorScheme.surface else Color.Transparent,
        label = "topBarColor"
    )
    val topBarContentColor by animateColorAsState(
        targetValue = if (isScrolled) MaterialTheme.colorScheme.onSurface else Color.White,
        label = "topBarContentColor"
    )
    val titleAlpha by animateFloatAsState(
        targetValue = if (isScrolled) 1f else 0f,
        label = "titleAlpha"
    )

    GizzScaffold(
        topAppBar = {
            TopAppBar(
                title = {
                    TopAppBarText(
                        title = title.title,
                        modifier = Modifier.graphicsLayer { alpha = titleAlpha }
                    )
                },
                navigationIcon = navigationUpIcon(navigateUp),
                actions = { platformActions() },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = topBarColor,
                    navigationIconContentColor = topBarContentColor,
                    actionIconContentColor = topBarContentColor,
                ),
            )
        },
        bottomBar = null,
    ) { innerPadding, onPlaybackError ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = innerPadding.calculateBottomPadding())
        ) {
            Box(modifier = Modifier.weight(1f)) {
                when (showState) {
                    LCE.Loading -> LoadingScreen()
                    is LCE.Error -> ErrorScreen(error = showState.error)
                    is LCE.Content -> ShowContent(
                        title = title,
                        state = showState.value,
                        playerState = playerState,
                        lazyListState = lazyListState,
                        onRecordingChange = onRecordingChange,
                        onPlayAll = {
                            showState.value.removeOldMediaItemsAndAddNew(0)
                            onPlayerClick(title)
                        },
                        onTrackClick = { index ->
                            showState.value.removeOldMediaItemsAndAddNew(index)
                            onPlayerClick(title)
                        }
                    )
                }
            }

            MiniPlayer(
                playerState = playerState,
                onClick = onMiniPlayerClick,
                playerActions = PlayerActions(
                    play = onPlayAction,
                    pause = onPauseAction
                ),
                playerError = onPlaybackError,
            )
        }
    }
}

@Composable
private fun ShowContent(
    title: FullShowTitle,
    state: ShowScreenState,
    playerState: PlayerState,
    lazyListState: LazyListState,
    onRecordingChange: (RecordingId) -> Unit,
    onPlayAll: () -> Unit,
    onTrackClick: (Int) -> Unit,
) {
    var showRecordingMenu by remember { mutableStateOf(false) }
    var showMetadata by remember { mutableStateOf(false) }

    LazyColumn(
        state = lazyListState,
        modifier = Modifier.fillMaxSize(),
    ) {
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(GizzTapesTheme.size.headerSize)
            ) {
                PosterImage(
                    posterUrl = state.showPosterUrl,
                    contentScale = ContentScale.FillBounds,
                    modifier = Modifier.matchParentSize()
                )
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.65f))
                            )
                        )
                )
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(horizontal = 16.dp, vertical = 16.dp)
                ) {
                    Text(
                        text = title.title.value,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                    )
                    Text(
                        text = title.date.toAlbumFormat(),
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White,
                    )
                }
            }
        }

        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(modifier = Modifier.weight(1f)) {
                    TextButton(onClick = { showRecordingMenu = true }) {
                        Text(state.recordingData.selectedRecording)
                        Icon(Icons.Default.ExpandMore, null)
                    }
                    DropdownMenu(
                        expanded = showRecordingMenu,
                        onDismissRequest = { showRecordingMenu = false }
                    ) {
                        state.recordingData.recordings.forEach { rec ->
                            DropdownMenuItem(
                                text = { Text(rec.id) },
                                onClick = {
                                    onRecordingChange(rec)
                                    showRecordingMenu = false
                                }
                            )
                        }
                    }
                }

                TextButton(onClick = onPlayAll) {
                    Icon(Icons.Default.PlayArrow, null)
                    Text(
                        text = "Play All",
                        softWrap = false
                    )
                }
            }
        }

        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showMetadata = !showMetadata }
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Recording Info", style = MaterialTheme.typography.labelLarge)
                Spacer(Modifier.weight(1f))
                Icon(
                    if (showMetadata) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = null
                )
            }
        }

        if (showMetadata) {
            item {
                ShowMetadata(recordingData = state.recordingData)
            }
        }

        item { HorizontalDivider() }

        itemsIndexed(state.tracks.toList()) { index, track ->
            TrackRow(
                track = track,
                isPlaying = playerState is PlayerState.MediaLoaded && playerState.mediaId == track.id && playerState.isPlaying,
                onClick = { onTrackClick(index) }
            )
        }
    }
}

@Composable
private fun TrackRow(
    track: ShowScreenState.Track,
    isPlaying: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
            contentDescription = if (isPlaying) "Pause" else "Play",
            tint = if (isPlaying) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
        )
        Spacer(Modifier.width(12.dp))
        Text(
            text = track.title.title,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = track.duration.formattedDuration,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun ShowMetadata(recordingData: RecordingData) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        recordingData.notes?.let {
            HtmlText(
                text = it,
                style = MaterialTheme.typography.bodySmall
            )
            Spacer(Modifier.height(4.dp))
        }
        recordingData.taper?.let {
            Text("Taper: $it", style = MaterialTheme.typography.bodySmall)
        }
        recordingData.source?.let {
            Text("Source: $it", style = MaterialTheme.typography.bodySmall)
        }
        recordingData.lineage?.let {
            Text("Lineage: $it", style = MaterialTheme.typography.bodySmall)
        }
        Text("Identifier: ${recordingData.identifier}", style = MaterialTheme.typography.bodySmall)
        Text("Uploaded: ${recordingData.uploadDate}", style = MaterialTheme.typography.bodySmall)
        Spacer(Modifier.height(4.dp))
        Text(
            buildAnnotatedString {
                withLink(
                    LinkAnnotation.Url(
                        recordingData.kglwNetShowLink,
                        TextLinkStyles(
                            SpanStyle(
                                color = MaterialTheme.colorScheme.primary,
                                textDecoration = TextDecoration.Underline
                            )
                        )
                    )
                ) {
                    append("View show on kglw.net")
                }
            },
            style = MaterialTheme.typography.bodySmall
        )
    }
}
