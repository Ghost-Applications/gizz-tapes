@file:OptIn(kotlin.time.ExperimentalTime::class)

package gizz.tapes.ui.show

import androidx.compose.foundation.basicMarquee
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
import androidx.compose.foundation.lazy.itemsIndexed
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
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withLink
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import dev.zacsweers.metrox.viewmodel.assistedMetroViewModel
import dev.zacsweers.metrox.viewmodel.metroViewModel
import gizz.tapes.LocalPlatformActions
import gizz.tapes.data.FullShowTitle
import gizz.tapes.data.RecordingData
import gizz.tapes.data.RecordingId
import gizz.tapes.data.ShowScreenState
import gizz.tapes.nav.NavigateUp
import gizz.tapes.ui.components.ErrorScreen
import gizz.tapes.ui.components.LoadingScreen
import gizz.tapes.ui.components.navigationUpIcon
import gizz.tapes.ui.player.MiniPlayer
import gizz.tapes.ui.player.PlayerState
import gizz.tapes.ui.player.PlayerViewModel
import gizz.tapes.util.LCE

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
    val scrollBehavior =
        TopAppBarDefaults.exitUntilCollapsedScrollBehavior(rememberTopAppBarState())
    val platformActions = LocalPlatformActions.current

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
                title = {
                    Text(
                        text = title.fullShowTitle.value,
                        maxLines = 1,
                        modifier = Modifier.basicMarquee(iterations = Int.MAX_VALUE)
                    )
                },
                navigationIcon = navigationUpIcon(navigateUp),
                actions = { platformActions() },
                scrollBehavior = scrollBehavior
            )
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            Box(modifier = Modifier.weight(1f)) {
                when (showState) {
                    LCE.Loading -> LoadingScreen()
                    is LCE.Error -> ErrorScreen(showState.userDisplayedMessage)
                    is LCE.Content -> ShowContent(
                        state = showState.value,
                        playerState = playerState,
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
                onPlayAction = onPlayAction,
                onPauseAction = onPauseAction,
                playerError = {}
            )
        }
    }
}

@Composable
private fun ShowContent(
    state: ShowScreenState,
    playerState: PlayerState,
    onRecordingChange: (RecordingId) -> Unit,
    onPlayAll: () -> Unit,
    onTrackClick: (Int) -> Unit,
) {
    var showRecordingMenu by remember { mutableStateOf(false) }
    var showMetadata by remember { mutableStateOf(false) }

    LazyColumn(modifier = Modifier.fillMaxSize()) {
        item {
            AsyncImage(
                model = state.showPosterUrl.value,
                contentDescription = "Show poster",
                contentScale = ContentScale.FillWidth,
                modifier = Modifier.fillMaxWidth().height(220.dp)
            )
        }

        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Recording selector
                Box {
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

                Spacer(Modifier.weight(1f))

                TextButton(onClick = onPlayAll) {
                    Icon(Icons.Default.PlayArrow, null)
                    Text("Play All")
                }
            }
        }

        // Metadata toggle
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
            Text(it, style = MaterialTheme.typography.bodySmall)
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
