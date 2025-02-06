package gizz.tapes.ui.player

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.navigation.toRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import gizz.tapes.data.FullShowTitle
import gizz.tapes.data.PlayerErrorMessage
import gizz.tapes.playback.MediaPlayerContainer
import gizz.tapes.ui.player.PlayerState.NoMedia
import gizz.tapes.util.MediaItemWrapper
import gizz.tapes.util.getDecodedFromString
import gizz.tapes.util.showExtras
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@UnstableApi
@HiltViewModel
class PlayerViewModel @Inject constructor(
    private val mediaPlayerContainer: MediaPlayerContainer,
    private val playerErrorMessage: PlayerErrorMessage,
    savedStateHandle: SavedStateHandle,
): ViewModel() {

    private lateinit var player: Player

    val title: FullShowTitle? = savedStateHandle.getDecodedFromString("showTitle")

    private fun playerCallbackFlow() = callbackFlow {
        val listener = object : Player.Listener {
            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                viewModelScope.launch {
                    send(newState())
                }
            }

            override fun onIsPlayingChanged(isPlaying: Boolean) {
                viewModelScope.launch {
                    send(newState())
                }
            }

            override fun onIsLoadingChanged(isLoading: Boolean) {
                viewModelScope.launch {
                    send(newState())
                }
            }

            override fun onPlayerError(error: PlaybackException) {
                viewModelScope.launch {
                    send(newState(PlayerError(playerErrorMessage.value)))
                }
            }
        }

        while (mediaPlayerContainer.mediaPlayer == null) {
            delay(10)
        }

        player = checkNotNull(mediaPlayerContainer.mediaPlayer)
        player.addListener(listener)

        awaitClose {
            player.removeListener(listener)
        }
    }

    private fun updatePlayerState(): Flow<PlayerState> {
        return flow {
            while(mediaPlayerContainer.mediaPlayer == null) {
                delay(100)
            }

            while (currentCoroutineContext().isActive && mediaPlayerContainer.mediaPlayer != null) {
                delay(1000)
                emit(newState())
            }
        }
    }

    val playerState = merge(playerCallbackFlow(), updatePlayerState()).stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = NoMedia
    )

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


    private fun newState(playerError: PlayerError? = null): PlayerState {
        val cmi = player.currentMediaItem
        return when {
            cmi == null -> NoMedia
            // somehow when working with the cast player we can see media items we didn't queue
            cmi.mediaId.isEmpty() -> NoMedia
            else -> {
                val metadata = cmi.mediaMetadata
                val (showId, venueName) = cmi.showExtras ?: run {
                    Timber.w(
                        "Current media item does not have required extras: %s",
                        MediaItemWrapper(cmi)
                    )
                    return NoMedia
                }

                if (playerError == null) {
                    PlayerState.MediaLoaded(
                        isPlaying = player.isPlaying,
                        isLoading = player.isLoading,
                        durationInfo = MediaDurationInfo(
                            currentPosition = player.currentPosition,
                            duration = player.duration
                        ),
                        showId = showId,
                        showTitle = venueName,
                        artworkUri = metadata.artworkUri,
                        title = metadata.title.toString(),
                        albumTitle = metadata.albumTitle.toString(),
                        mediaId = cmi.mediaId,
                    )
                } else {
                    PlayerState.MediaLoaded.Error(
                        playerError = playerError,
                        durationInfo = MediaDurationInfo(
                            currentPosition = player.currentPosition,
                            duration = player.duration
                        ),
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
}
