package gizz.tapes.ui.show

import androidx.datastore.core.DataStore
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.navigation.toRoute
import arrow.core.toNonEmptyListOrNull
import co.touchlab.kermit.Logger
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Assisted
import dev.zacsweers.metro.AssistedFactory
import dev.zacsweers.metro.AssistedInject
import dev.zacsweers.metro.ContributesIntoMap
import dev.zacsweers.metrox.viewmodel.ViewModelAssistedFactory
import dev.zacsweers.metrox.viewmodel.ViewModelAssistedFactoryKey
import gizz.tapes.api.GizzTapesApiClient
import gizz.tapes.api.data.Show
import gizz.tapes.data.ApiErrorMessage
import gizz.tapes.data.FullShowTitle
import gizz.tapes.data.PosterUrl
import gizz.tapes.data.RecordingData
import gizz.tapes.data.RecordingId
import gizz.tapes.data.Settings
import gizz.tapes.data.ShowId
import gizz.tapes.data.ShowScreenState
import gizz.tapes.data.TrackDuration
import gizz.tapes.data.TrackTitle
import gizz.tapes.nav.Destination
import gizz.tapes.playback.GizzMediaPlayer
import gizz.tapes.playback.PlaybackItem
import gizz.tapes.util.ForViewModel
import gizz.tapes.util.LCE
import gizz.tapes.util.map
import gizz.tapes.util.retryUntilSuccessful
import gizz.tapes.util.tryAndGetPreferredRecordingType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@AssistedInject
class ShowViewModel(
    private val apiClient: GizzTapesApiClient,
    private val mediaPlayer: GizzMediaPlayer,
    private val apiErrorMessage: ApiErrorMessage,
    private val datastore: DataStore<Settings>,
    @Assisted savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val showRoute = savedStateHandle.toRoute<Destination.Show>(Destination.Show.typeMap)
    private val showId: ShowId = showRoute.id
    val title: FullShowTitle = showRoute.title

    private val logger = Logger.withTag("ShowViewModel")
    private val cachedShowData = MutableStateFlow<LCE<Show, Nothing>>(LCE.Loading)
    private val errorFlow = MutableStateFlow<LCE.Error<Throwable>?>(null)
    private val selectedRecording = MutableStateFlow<RecordingId?>(null)

    val show: StateFlow<LCE<ShowScreenState, Throwable>> = loadShow().stateIn(
        scope = viewModelScope,
        started = SharingStarted.ForViewModel,
        initialValue = LCE.Loading
    )

    init {
        viewModelScope.launch { fetchAndCacheShow() }
    }

    fun changeSelectedRecording(recordingId: RecordingId) {
        selectedRecording.value = recordingId
    }

    private suspend fun fetchAndCacheShow() {
        val data = retryUntilSuccessful(
            action = { apiClient.show(showId.value) },
            onErrorAfter3SecondsAction = { error ->
                logger.d(error) { "Error retrieving show" }
                errorFlow.emit(
                    LCE.Error(userDisplayedMessage = apiErrorMessage.value, error = error)
                )
            }
        )
        cachedShowData.emit(data)
    }

    private fun loadShow(): Flow<LCE<ShowScreenState, Throwable>> {
        return combine(
            selectedRecording,
            datastore.data,
            cachedShowData
        ) { selRec, settings, showData ->
            Triple(selRec, settings.preferredRecordingType, showData)
        }.map { (selRec, preferredRecording, showData) ->
            val error = errorFlow.value
            if (error != null && showData == LCE.Loading) {
                return@map error
            }

            showData.map { show ->
                val recording = show.recordings.firstOrNull { it.id == selRec?.id }
                    ?: show.recordings.tryAndGetPreferredRecordingType(preferredRecording)

                val playbackItems = recording.files.map { track ->
                    PlaybackItem(
                        id = "${recording.id}/${track.filename}",
                        url = recording.filesPathPrefix + track.filename,
                        title = track.title,
                        albumTitle = title.title.value,
                        artworkUrl = PosterUrl.Companion(show.posterUrl).value,
                        showId = ShowId(show.id),
                        showTitle = title,
                        durationMs = track.length.inWholeMilliseconds,
                        showDate = show.date
                    )
                }

                ShowScreenState(
                    showPosterUrl = PosterUrl.Companion(show.posterUrl),
                    removeOldMediaItemsAndAddNew = { startIndex ->
                        viewModelScope.launch {
                            mediaPlayer.setPlaylist(playbackItems, startIndex)
                        }
                    },
                    tracks = recording.files.map { track ->
                        ShowScreenState.Track(
                            id = "${recording.id}/${track.filename}",
                            title = TrackTitle(track.title),
                            duration = TrackDuration(track.length)
                        )
                    }.toNonEmptyListOrNull()!!,
                    recordingData = RecordingData(
                        notes = show.notes,
                        selectedRecording = recording.id,
                        recordings = show.recordings.map { RecordingId(it.id) },
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

    @AssistedFactory
    @ViewModelAssistedFactoryKey(ShowViewModel::class)
    @ContributesIntoMap(AppScope::class)
    fun interface Factory : ViewModelAssistedFactory {
        fun create(savedStateHandle: SavedStateHandle): ShowViewModel
        override fun create(extras: CreationExtras): ShowViewModel = create(extras.createSavedStateHandle())
    }
}
