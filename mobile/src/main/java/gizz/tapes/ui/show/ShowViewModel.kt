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
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

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

    val show: StateFlow<LCE<ShowScreenState, Throwable>> = loadShow()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = LCE.Loading
        )

    @OptIn(UnstableApi::class)
    private fun loadShow(): Flow<LCE<ShowScreenState, Throwable>> {
        return flow {
            val preferredRecording = datastore.data
                .map { it.preferredRecordingType }
                .first()

            val state: LCE<ShowScreenState, Nothing> = retryUntilSuccessful(
                action = {
                    apiClient.show(showId)
                },
                onErrorAfter3SecondsAction = { error ->
                    Timber.d(error, "Error retrieving show")
                    emit(
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
                        viewModelScope.launch {
                            checkNotNull(mediaPlayerContainer.mediaPlayer).apply {
                                clearMediaItems()
                                addMediaItems(items)
                            }
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

            emit(state)
        }
    }
}
