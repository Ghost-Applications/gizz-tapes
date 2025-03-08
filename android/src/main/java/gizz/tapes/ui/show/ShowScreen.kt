@file:OptIn(ExperimentalMaterial3Api::class)

package gizz.tapes.ui.show

import android.text.method.LinkMovementMethod
import android.text.util.Linkify
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.text.HtmlCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.media3.common.util.UnstableApi
import arrow.core.nonEmptyListOf
import coil.compose.AsyncImage
import com.google.android.material.textview.MaterialTextView
import gizz.tapes.R
import gizz.tapes.data.FullShowTitle
import gizz.tapes.ui.components.CastButton
import gizz.tapes.ui.components.ErrorScreen
import gizz.tapes.ui.components.LoadingScreen
import gizz.tapes.ui.components.navigationUpIcon
import gizz.tapes.ui.nav.NavigateUp
import gizz.tapes.ui.player.MiniPlayer
import gizz.tapes.ui.player.PlayerError
import gizz.tapes.ui.player.PlayerState
import gizz.tapes.ui.player.PlayerState.MediaLoaded
import gizz.tapes.ui.player.PlayerState.NoMedia
import gizz.tapes.ui.player.PlayerViewModel
import gizz.tapes.ui.theme.GizzTheme
import gizz.tapes.util.LCE
import gizz.tapes.util.contentOrNull
import gizz.tapes.util.onContent
import gizz.tapes.util.toAlbumFormat
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.seconds

@UnstableApi
@Composable
fun ShowScreen(
    viewModel: ShowViewModel = hiltViewModel(),
    playerViewModel: PlayerViewModel = hiltViewModel(),
    navigateUp: NavigateUp,
    onMiniPlayerClick: (FullShowTitle) -> Unit
) {
    val showState by viewModel.show.collectAsState()
    val playerState: PlayerState by playerViewModel.playerState.collectAsState()

    var firstLoad by remember { mutableStateOf(true) }

    ShowScreen(
        state = showState,
        playerState = playerState,
        fullShowTitle = viewModel.title,
        navigateUp = navigateUp,
        onMiniPlayerClick = onMiniPlayerClick,
        onPauseAction = playerViewModel::pause,
        onPlayAction = playerViewModel::play,
        actions = { CastButton() },
        onRowClick = { index, isPlaying ->
            if (firstLoad) {
                firstLoad = false
                val content = checkNotNull(showState.contentOrNull())
                content.removeOldMediaItemsAndAddNew()
                playerViewModel.seekTo(index, 0)
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
        },
        recordingChanged = { rId -> viewModel.changeSelectedRecording(rId) }
    )
}

@Composable
fun ShowScreen(
    state: LCE<ShowScreenState, Throwable>,
    playerState: PlayerState,
    fullShowTitle: FullShowTitle,
    navigateUp: NavigateUp,
    onRowClick: (index: Int, isPlaying: Boolean) -> Unit,
    onMiniPlayerClick: (FullShowTitle) -> Unit,
    onPauseAction: () -> Unit,
    onPlayAction: () -> Unit,
    recordingChanged: (RecordingId) -> Unit,
    actions: @Composable RowScope.() -> Unit,
) {

    val snackbarHostState = remember { SnackbarHostState() }
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(rememberTopAppBarState())
    val isCollapsed by remember { derivedStateOf { scrollBehavior.state.collapsedFraction > 0.5 } }
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.primary
                ),
                title = {
                    Row {
                        if (!isCollapsed) {
                            state.onContent {
                                AsyncImage(
                                    model = it.showPosterUrl.value,
                                    contentDescription = null,
                                    contentScale = ContentScale.FillHeight,
                                    modifier = Modifier.padding(8.dp)
                                )
                            }
                        }

                        Column(
                            modifier = Modifier.clickable {
                                scope.launch {
                                    listState.animateScrollToItem(0)
                                    scrollBehavior.state.heightOffset = 1f
                                }
                            }.padding(start = 8.dp)
                        ) {
                            Text(
                                text = fullShowTitle.date.toAlbumFormat(),
                                maxLines = 1
                            )

                            if (!isCollapsed) {
                                Text(
                                    text = fullShowTitle.title.value,
                                    maxLines = 1,
                                    style = MaterialTheme.typography.titleSmall,
                                    overflow = TextOverflow.Visible,
                                    modifier = Modifier.basicMarquee(
                                        iterations = Int.MAX_VALUE
                                    ),
                                )
                            }
                        }
                    }
                },
                navigationIcon = navigationUpIcon(navigateUp),
                actions = actions,
                scrollBehavior = scrollBehavior
            )
        },
    ) { innerPadding ->
        AnimatedContent(
            label = "Show Screen Scaffold",
            targetState = state,
            contentKey = { current ->
                when(current) {
                    is LCE.Content -> "Content"
                    is LCE.Error -> "Error"
                    LCE.Loading -> "Loading"
                }
            }
        ) { s ->
            when(s) {
                is LCE.Content -> ShowListWithPlayer(
                    showData = s.value,
                    onMiniPlayerClick = onMiniPlayerClick,
                    onRowClick = onRowClick,
                    onPauseAction = onPauseAction,
                    onPlayAction = onPlayAction,
                    playerState = playerState,
                    playerError = {
                        scope.launch {
                            snackbarHostState.showSnackbar(
                                it.message,
                                duration = SnackbarDuration.Long
                            )
                        }
                    },
                    modifier = Modifier.padding(innerPadding),
                    recordingChanged = recordingChanged
                )
                is LCE.Error -> ErrorScreen(s.userDisplayedMessage)
                LCE.Loading -> LoadingScreen()
            }
        }
    }
}

@Composable
fun ShowListWithPlayer(
    showData: ShowScreenState,
    playerState: PlayerState,
    onRowClick: (index: Int, isPlaying: Boolean) -> Unit,
    onMiniPlayerClick: (FullShowTitle) -> Unit,
    onPauseAction: () -> Unit,
    onPlayAction: () -> Unit,
    playerError: (PlayerError) -> Unit,
    recordingChanged: (RecordingId) -> Unit,
    modifier: Modifier = Modifier
) {
    val (currentlyPlayingMediaId, playing) = when(playerState) {
        is MediaLoaded -> playerState.mediaId to playerState.isPlaying
        NoMedia -> "" to false
    }

    Column(
        modifier = modifier,
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .weight(1f)
        ) {
            item {
                ShowHeader(
                    recordingData = showData.recordingData,
                    recordingChanged = recordingChanged
                )
            }

            itemsIndexed(showData.tracks) { i, track ->
                val isPlaying = track.id.id == currentlyPlayingMediaId && playing

                TrackRow(
                    trackTitle = track.title,
                    duration = track.duration,
                    playing = isPlaying,
                    onClick = { onRowClick(i, isPlaying) }
                )
            }

            item { ShowFooter(recordingData = showData.recordingData) }
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
fun ShowHeader(recordingData: RecordingData, recordingChanged: (RecordingId) -> Unit) {
    var showMenu by remember { mutableStateOf(false) }

    Column(modifier = Modifier.padding(16.dp)) {
        Row(
            modifier = Modifier.clickable { showMenu = !showMenu }
        ) {
            Text(recordingData.selectedRecording)
            Icon(
                imageVector = Icons.Default.ArrowDropDown,
                contentDescription = null
            )

            DropdownMenu(
                expanded = showMenu,
                onDismissRequest = { showMenu = false }
            ) {

                recordingData.recordings.forEach { r ->
                    DropdownMenuItem(
                        onClick = {
                            recordingChanged(r)
                            showMenu = false
                        },
                        text = { Text(r.id) }
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        recordingData.notes?.let { notes ->
            val spannedNotes = HtmlCompat.fromHtml(notes, HtmlCompat.FROM_HTML_MODE_COMPACT)
            AndroidView(
                factory = {
                    MaterialTextView(it).apply {
                        autoLinkMask = Linkify.WEB_URLS and Linkify.EMAIL_ADDRESSES
                        linksClickable = true
                        movementMethod = LinkMovementMethod.getInstance()
                        text = spannedNotes
                    }
                }
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun ShowHeaderPreview() {
    GizzTheme {
        ShowHeader(
            recordingData = RecordingData(
                notes = "Rattlesnake contained Nuclear Fusion, O.N.E. and Automation teases and Honey teases & quotes. Ice V contained Mirage City teases. Hog Calling Contest featured Joey on acoustic guitar. Following Set Amby quoted She'll Be Coming 'Round the Mountain (traditional). Magenta Mountain contained The Grim Reaper quotes.",
                selectedRecording = "SBD: kglw2020-11-20.bandcampbootlegger",
                recordings = nonEmptyListOf(
                    RecordingId("SBD: kglw2020-11-20.bandcampbootlegger"),
                    RecordingId("AUD by Archie Cove: kglw2024-11-20archie")
                ),
                taper = "",
                source = "",
                lineage = "",
                identifier = "",
                uploadDate = "",
            )
        ) {}
    }
}

@Composable
fun ShowFooter(recordingData: RecordingData) {
    Column(modifier = Modifier.padding(16.dp)) {
        recordingData.taper?.let { ShowFooterRow("Taper", it) }
        recordingData.source?.let { ShowFooterRow("Source", it) }
        recordingData.lineage?.let { ShowFooterRow("Lineage", it) }
        ShowFooterRow("Identifier", recordingData.identifier)
        ShowFooterRow("Upload Date", recordingData.uploadDate)
    }
}

@Composable
fun ShowFooterRow(label: String, content: String) {
    Text(
        text = label,
        style = MaterialTheme.typography.titleSmall
    )
    Text(
        text = content,
        style = MaterialTheme.typography.bodySmall
    )
    Spacer(modifier = Modifier.height(8.dp))
}

@Preview(showBackground = true)
@Composable
fun ShowFooterPreview() {
    GizzTheme {
        ShowFooter(
            recordingData = RecordingData(
                notes = "Rattlesnake contained Nuclear Fusion, O.N.E. and Automation teases and Honey teases & quotes. Ice V contained Mirage City teases. Hog Calling Contest featured Joey on acoustic guitar. Following Set Amby quoted She'll Be Coming 'Round the Mountain (traditional). Magenta Mountain contained The Grim Reaper quotes.",
                selectedRecording = "SBD: kglw2020-11-20.bandcampbootlegger",
                taper = "Sam Joseph, Joe Santarpia, Grace Reader, Gaspard De Meulemeester",
                source = "SBD",
                lineage = "SBD > Bandcamp",
                identifier = "kglw2020-11-20.bandcampbootlegger",
                uploadDate = "2024-11-27 12:52:26",
                recordings = nonEmptyListOf(RecordingId("AUD by Archie Cove: kglw2024-11-20archie"))
            )
        )
    }
}

@Composable
fun TrackRow(
    trackTitle: TrackTitle,
    duration: TrackDuration,
    playing: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .padding(horizontal = 4.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(if (playing) MaterialTheme.colorScheme.surfaceContainerHighest else MaterialTheme.colorScheme.surfaceContainer)
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
                .background(MaterialTheme.colorScheme.secondaryContainer)
        ) {
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
                    tint = MaterialTheme.colorScheme.onSecondaryContainer
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
    Spacer(modifier = Modifier.height(4.dp))
}

@Preview(showBackground = true)
@Composable
fun TrackRowPreview() {
    GizzTheme {
        TrackRow(
            trackTitle = TrackTitle("Hypertension"),
            duration = TrackDuration((10 * 60).seconds),
            playing = false
        ) {

        }
    }
}
