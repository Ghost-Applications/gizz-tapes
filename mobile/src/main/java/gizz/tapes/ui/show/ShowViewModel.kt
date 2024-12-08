package gizz.tapes.ui.show

import android.os.Bundle
import androidx.annotation.OptIn
import androidx.datastore.core.DataStore
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.MimeTypes
import androidx.media3.common.util.UnstableApi
import arrow.core.NonEmptyList
import dagger.hilt.android.lifecycle.HiltViewModel
import gizz.tapes.api.GizzTapesApiClient
import gizz.tapes.data.ApiErrorMessage
import gizz.tapes.data.BandName
import gizz.tapes.data.PosterUrl
import gizz.tapes.data.Settings
import gizz.tapes.data.Title
import gizz.tapes.playback.MediaPlayerContainer
import gizz.tapes.ui.show.ShowScreenState.Track
import gizz.tapes.util.LCE
import gizz.tapes.util.map
import gizz.tapes.util.retryUntilSuccessful
import gizz.tapes.util.tryAndGetPreferredRecordingType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes

data class ShowScreenState(
    val removeOldMediaItemsAndAddNew: () -> Unit,
    val showPosterUrl: PosterUrl,
    val tracks: NonEmptyList<Track>
) {
    data class Track(
        val id: TrackId,
        val title: TrackTitle,
        val duration: TrackDuration
    )
}

@JvmInline
value class TrackId(val id: String)

@JvmInline
value class TrackTitle(val title: String)

@JvmInline
value class TrackDuration(val duration: Duration) {
    val formatedDuration: String get() {
        val seconds = duration.inWholeSeconds - duration.inWholeMinutes.minutes.inWholeSeconds
        val secondsString = seconds.toString().takeIf { it.length > 1 } ?: "0$seconds"

        return "${duration.inWholeMinutes}:$secondsString"
    }
}

@HiltViewModel
class ShowViewModel @Inject constructor(
    private val apiClient: GizzTapesApiClient,
    private val mediaPlayerContainer: MediaPlayerContainer,
    private val apiErrorMessage: ApiErrorMessage,
    private val datastore: DataStore<Settings>,
    savedStateHandle: SavedStateHandle
): ViewModel() {

    private val showId: String = checkNotNull(savedStateHandle["id"])

    val title: Title = Title.fromEncodedString(
        checkNotNull(savedStateHandle.get<String>("title"))
    )

    private val _show: MutableStateFlow<LCE<ShowScreenState, Throwable>> = MutableStateFlow(LCE.Loading)
    val show: StateFlow<LCE<ShowScreenState, Throwable>> = _show

    init {
        loadShow()
    }

    @OptIn(UnstableApi::class)
    private fun loadShow() {
        viewModelScope.launch {
            val preferredRecording = datastore.data
                .map { it.preferredRecordingType }
                .first()

            val state: LCE<ShowScreenState, Nothing> = retryUntilSuccessful(
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
                val recording = show.recordings.tryAndGetPreferredRecordingType(preferredRecording)
                val items = recording.files.map { track ->
                    MediaItem.Builder()
                        .setUri(recording.filesPathPrefix + track.filename)
                        .setMediaId(track.filename)
                        .setMimeType(MimeTypes.AUDIO_MPEG)
                        .setMediaMetadata(
                            MediaMetadata.Builder()
                                .setExtras(
                                    Bundle().apply {
                                        putString("showId", show.id)
                                        putString("showTitle", title.value)
                                    }
                                )
                                .setArtist(BandName)
                                .setAlbumArtist(BandName)
                                .setAlbumTitle(title.value)
                                .setTitle(track.title)
                                .setRecordingYear(show.date.year)
                                .setArtworkUri(PosterUrl(show.posterUrl).toUri())
                                .setDurationMs(track.length.inWholeMilliseconds)
                                .setMediaType(MediaMetadata.MEDIA_TYPE_MUSIC)
                                .setIsPlayable(true)
                                .setIsBrowsable(false)
                                .build()
                        )
                        .build()
                }

                ShowScreenState(
                    showPosterUrl = PosterUrl(show.posterUrl),
                    removeOldMediaItemsAndAddNew = {
                        checkNotNull(mediaPlayerContainer.mediaPlayer).apply {
                            clearMediaItems()
                            addMediaItems(items)
                        }
                    },
                    tracks = recording.files.map {
                        Track(
                            id = TrackId(it.filename),
                            title = TrackTitle(it.title),
                            duration = TrackDuration(it.length)
                        )
                    }
                )
            }

            _show.emit(state)
        }
    }
}
