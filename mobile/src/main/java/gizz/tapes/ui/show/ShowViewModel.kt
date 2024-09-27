package gizz.tapes.ui.show

import android.net.Uri
import androidx.annotation.OptIn
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.MimeTypes
import androidx.media3.common.util.UnstableApi
import dagger.hilt.android.lifecycle.HiltViewModel
import gizz.tapes.api.GizzTapesApiClient
import gizz.tapes.api.data.Recording
import gizz.tapes.api.data.Show
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import gizz.tapes.playback.MediaPlayerContainer
import gizz.tapes.ui.ApiErrorMessage
import gizz.tapes.util.LCE
import gizz.tapes.util.map
import gizz.tapes.util.retryUntilSuccessful
import gizz.tapes.util.showTitle
import gizz.tapes.util.toAlbumFormat
import timber.log.Timber
import javax.inject.Inject
import kotlin.time.Duration.Companion.seconds

@HiltViewModel
class ShowViewModel @Inject constructor(
    private val apiClient: GizzTapesApiClient,
    private val mediaPlayerContainer: MediaPlayerContainer,
    private val apiErrorMessage: ApiErrorMessage,
    savedStateHandle: SavedStateHandle
): ViewModel() {

    private val showId: String = checkNotNull(savedStateHandle["id"])
    private val venue: String = checkNotNull(savedStateHandle["venue"])

    private val _appBarTitle: MutableStateFlow<String> = MutableStateFlow(venue)
    val appBarTitle: StateFlow<String> = _appBarTitle

    private val _show: MutableStateFlow<LCE<Show, Throwable>> = MutableStateFlow(LCE.Loading)
    val show: StateFlow<LCE<Show, Throwable>> = _show

    init {
        loadShow()
    }

    @OptIn(UnstableApi::class)
    private fun loadShow() {
        viewModelScope.launch {
            val state: LCE<Show, Nothing> = retryUntilSuccessful(
                action = {
                    apiClient.show(showId)
                },
                onErrorAfter3SecondsAction = { error ->
                    Timber.d(error, "Error retrieving show")
                    _show.emit(
                        LCE.Error(
                            userDisplayedMessage = apiErrorMessage.value,
                            error = error
                        )
                    )
                }
            ).map { show ->
                val recording = show.recordings.firstOrNull { it.type == Recording.Type.SBD } ?: show.recordings.first()

                val items = recording.files.map { track ->
                    MediaItem.Builder()
                        .setUri(recording.filesPathPrefix + track.filename)
                        .setMediaId(track.filename)
                        .setMimeType(MimeTypes.AUDIO_MPEG)
                        .setMediaMetadata(
                            MediaMetadata.Builder()
//                                .setExtras(show.toMetadataExtras()) TODO
                                .setArtist("King Gizzard & The Lizard Wizard")
                                .setAlbumArtist("King Gizzard & The Lizard Wizard")
                                .setAlbumTitle(show.showTitle)
                                .setTitle(track.title)
                                .setRecordingYear(show.date.year)
                                .setArtworkUri(show.posterUrl?.let { Uri.parse(it) })
                                .setDurationMs(track.length.toDouble().seconds.inWholeMilliseconds)
                                .setMediaType(MediaMetadata.MEDIA_TYPE_MUSIC)
                                .setIsPlayable(true)
                                .setIsBrowsable(false)
                                .build()
                        )
                        .build()
                }

                checkNotNull(mediaPlayerContainer.mediaPlayer).addMediaItems(items)
                viewModelScope.launch {
                    _appBarTitle.emit("${show.date.toAlbumFormat()}") // TODO figure out how to get venue name ${show.venue_name}")
                }

                show
            }

            _show.emit(state)
        }
    }
}