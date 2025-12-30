@file:kotlin.OptIn(ExperimentalTime::class)

package gizz.tapes.ui.show

import androidx.annotation.OptIn
import androidx.datastore.core.DataStore
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.MimeTypes
import androidx.media3.common.util.UnstableApi
import androidx.navigation.toRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import gizz.tapes.api.GizzTapesApiClient
import gizz.tapes.data.ApiErrorMessage
import gizz.tapes.data.BandName
import gizz.tapes.data.FullShowTitle
import gizz.tapes.data.MediaId
import gizz.tapes.data.PosterUrl
import gizz.tapes.data.Settings
import gizz.tapes.playback.MediaPlayerContainer
import gizz.tapes.ui.nav.Show
import gizz.tapes.ui.show.ShowScreenState.Track
import gizz.tapes.util.LCE
import gizz.tapes.util.map
import gizz.tapes.util.retryUntilSuccessful
import gizz.tapes.util.setMediaId
import gizz.tapes.util.toExtrasBundle
import gizz.tapes.util.tryAndGetPreferredRecordingType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate
import kotlinx.datetime.number
import timber.log.Timber
import javax.inject.Inject
import kotlin.time.ExperimentalTime
import gizz.tapes.api.data.Show as ApiShow

@HiltViewModel
class ShowViewModel @Inject constructor(
    private val apiClient: GizzTapesApiClient,
    private val mediaPlayerContainer: MediaPlayerContainer,
    private val apiErrorMessage: ApiErrorMessage,
    private val datastore: DataStore<Settings>,
    savedStateHandle: SavedStateHandle
): ViewModel() {

    private val showRoute = savedStateHandle.toRoute<Show>(Show.typeMap)
    private val showId = showRoute.id

    private val cachedShowData = MutableStateFlow<LCE<ApiShow, Nothing>>(LCE.Loading)
    private val errorFlow = MutableStateFlow<LCE.Error<Throwable>?>(null)
    private val selectedRecording = MutableStateFlow<RecordingId?>(null)

    val title: FullShowTitle = showRoute.title

    val show: StateFlow<LCE<ShowScreenState, Throwable>> = loadShow()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = LCE.Loading
        )

    fun changeSelectedRecording(recordingId: RecordingId) {
        selectedRecording.value = recordingId
    }

    private suspend fun fetchAndCacheShow() {
        val data: LCE.Content<ApiShow> = retryUntilSuccessful(
            action = {
                apiClient.show(showId.value)
            },
            onErrorAfter3SecondsAction = { error ->
                Timber.d(error, "Error retrieving show")
                errorFlow.emit(
                    LCE.Error(
                        userDisplayedMessage = apiErrorMessage.value,
                        error = error
                    )
                )
            }
        )

        cachedShowData.emit(data)
    }

    @OptIn(UnstableApi::class)
    private fun loadShow(): Flow<LCE<ShowScreenState, Throwable>> {
        return combine(selectedRecording, datastore.data, cachedShowData) { selectedRecording, settings, showData ->
            Triple(selectedRecording, settings.preferredRecordingType, showData)
        }.map { (selectedRecording, preferredRecording, showData) ->
            if (cachedShowData.value == LCE.Loading) {
                fetchAndCacheShow()
            }

            showData.map { show ->
                val recording = show.recordings.firstOrNull { it.id == selectedRecording?.id }
                        ?: show.recordings.tryAndGetPreferredRecordingType(preferredRecording)

                val items = recording.files.map { track ->
                    MediaItem.Builder()
                        .setUri(recording.filesPathPrefix + track.filename)
                        .setMediaId(
                            MediaId.TrackId(
                                show = show,
                                file = track,
                                recording = recording
                            )
                        )
                        .setMimeType(MimeTypes.AUDIO_MPEG)
                        .setMediaMetadata(
                            MediaMetadata.Builder()
                                .setExtras(showRoute.toExtrasBundle())
                                .setArtist(BandName)
                                .setAlbumArtist(BandName)
                                .setAlbumTitle(title.title.value)
                                .setTitle(track.title)
                                .setRecordingYear(show.date.year)
                                .setRecordingMonth(show.date.month.number)
                                .setRecordingDay(show.date.day)
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
                            id = MediaId.TrackId(recording = recording, show = show, file = it),
                            title = TrackTitle(it.title),
                            duration = TrackDuration(it.length)
                        )
                    },
                    recordingData = RecordingData(
                        notes = show.notes,
                        selectedRecording = recording.id,
                        recordings = show.recordings.map { RecordingId(it.id) }.toNonEmptyList(),
                        taper = recording.taper,
                        source = recording.source,
                        lineage = recording.lineage,
                        identifier = recording.id,
                        uploadDate = recording.uploadedAt.toString(),
                        kglwNetShowLink = show.kglwNet.fullLink
                    )
                )
            }
        }
    }
}
