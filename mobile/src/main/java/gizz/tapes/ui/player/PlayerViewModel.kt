package gizz.tapes.ui.player

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import gizz.tapes.playback.MediaPlayerContainer
import gizz.tapes.ui.data.Title
import gizz.tapes.ui.player.PlayerState.NoMedia
import gizz.tapes.util.formatedElapsedTime
import gizz.tapes.util.mediaExtras
import javax.inject.Inject

@UnstableApi
@HiltViewModel
class PlayerViewModel @Inject constructor(
    private val mediaPlayerContainer: MediaPlayerContainer,
    savedStateHandle: SavedStateHandle,
): ViewModel() {

    private lateinit var player: Player

    private val playerListener = object : Player.Listener {
        override fun onMediaMetadataChanged(mediaMetadata: MediaMetadata) {
            super.onMediaMetadataChanged(mediaMetadata)
        }

        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            viewModelScope.launch {
                _playerState.emit(newState())
            }
        }

        override fun onIsPlayingChanged(isPlaying: Boolean) {
            viewModelScope.launch {
                _playerState.emit(newState())
            }
        }
    }

    private val _playerState: MutableStateFlow<PlayerState> = MutableStateFlow(NoMedia)
    val playerState: StateFlow<PlayerState> = _playerState

    val title: Title? = savedStateHandle.get<String>("title")?.let { Title.fromEncodedString(it) }

    init {
        viewModelScope.launch {
            flow {
                while(mediaPlayerContainer.mediaPlayer == null) {
                    delay(1)
                }
                emit(checkNotNull(mediaPlayerContainer.mediaPlayer))
            }.collect {
                player = it
                it.addListener(playerListener)

                while (true) {
                    delay(1000)
                    _playerState.emit(newState())
                }
            }
        }
    }

    fun play() = player.play()
    fun pause() = player.pause()
    fun seekToPreviousMediaItem() = player.seekToPreviousMediaItem()
    fun seekToNextMediaItem() = player.seekToNextMediaItem()

    val mediaItemCount: Int get() = player.mediaItemCount
    fun getMediaItemAt(i: Int) = player.getMediaItemAt(i)
    fun removeMediaItems(fromIndex: Int, toIndex: Int) = player.removeMediaItems(fromIndex, toIndex)

    fun seekTo(mediaItemIndex: Int, positionMs: Long) {
        // make sure we are able to seek ahead
        // this is an issue when switching between local player and cast player
        viewModelScope.launch {
            while (!player.isCommandAvailable(Player.COMMAND_SEEK_TO_MEDIA_ITEM)) {
                delay(100)
            }
            player.seekTo(mediaItemIndex, positionMs)
        }
    }

    fun seekTo(positionMs: Long) = player.seekTo(positionMs)

    override fun onCleared() {
        player.removeListener(playerListener)
    }

    private fun newState(): PlayerState {
        val cmi = player.currentMediaItem
        return when {
            cmi == null -> NoMedia
            // somehow when working with the cast player we can media items we didn't queue
            cmi.mediaId.isEmpty() -> NoMedia
            else -> {
                val metadata = cmi.mediaMetadata
                val (showId, venueName) = cmi.mediaExtras

                PlayerState.MediaLoaded(
                    isPlaying = player.isPlaying,
                    formatedElapsedTime = player.formatedElapsedTime,
                    formatedDurationTime = metadata.durationMs?.formatedElapsedTime.orEmpty(),
                    duration = player.duration,
                    currentPosition = player.currentPosition,
                    showId = showId,
                    showTitle = venueName,
                    artworkUri = metadata.artworkUri,
                    title = metadata.title.toString(),
                    albumTitle = metadata.albumTitle.toString(),
                    mediaId = cmi.mediaId,
                )
            }
        }
    }
}