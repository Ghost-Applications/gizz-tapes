package gizz.tapes.playback

import android.content.ComponentName
import android.content.Context
import androidx.core.content.ContextCompat
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import co.touchlab.kermit.Logger
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import gizz.tapes.ui.player.MediaDurationInfo
import gizz.tapes.ui.player.PlayerError
import gizz.tapes.ui.player.PlayerState
import gizz.tapes.util.showExtras
import gizz.tapes.util.toMediaItems
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

private const val POLLING_INTERVAL_MS = 500L

@Inject
@ContributesBinding(AppScope::class)
@SingleIn(AppScope::class)
class AndroidMediaPlayer(context: Context) : GizzMediaPlayer {

    private val _state = MutableStateFlow<PlayerState>(PlayerState.NoMedia)
    override val state: StateFlow<PlayerState> = _state.asStateFlow()

    override val currentPosition: Long get() = mediaController?.currentPosition ?: 0L

    @Volatile private var mediaController: MediaController? = null
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var pollingJob: Job? = null

    init {
        val appContext = context.applicationContext
        val sessionToken = SessionToken(appContext, ComponentName(appContext, "gizz.tapes.PlaybackService"))
        val controllerFuture = MediaController.Builder(appContext, sessionToken).buildAsync()
        controllerFuture.addListener({
            val controller = try {
                controllerFuture.get()
            } catch (@Suppress("TooGenericExceptionCaught") e: Exception) {
                Logger.e(e) { "Error getting media controller" }
                return@addListener
            }
            mediaController = controller

            controller.addListener(object : Player.Listener {
                override fun onIsPlayingChanged(isPlaying: Boolean) {
                    updateState()
                    if (isPlaying) {
                        startPolling()
                    } else {
                        stopPolling()
                    }
                }

                override fun onPlaybackStateChanged(state: Int) = updateState()

                override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) =
                    updateState()

                override fun onPlayerError(error: PlaybackException) {
                    updateStateWithError(error.localizedMessage ?: "Playback error")
                }
            })
        }, ContextCompat.getMainExecutor(appContext))
    }

    private fun startPolling() {
        pollingJob?.cancel()
        pollingJob = scope.launch {
            while (true) {
                delay(POLLING_INTERVAL_MS)
                if (mediaController?.isPlaying == true) {
                    updateState()
                }
            }
        }
    }

    private fun stopPolling() {
        pollingJob?.cancel()
        pollingJob = null
    }

    private fun updateState() {
        val controller = mediaController ?: return
        val cmi = controller.currentMediaItem
        if (cmi == null || cmi.mediaId.isEmpty()) {
            _state.value = PlayerState.NoMedia
            return
        }
        val show = cmi.showExtras ?: run {
            _state.value = PlayerState.NoMedia
            return
        }
        val metadata = cmi.mediaMetadata
        val duration = if (controller.duration == C.TIME_UNSET) {
            metadata.durationMs ?: 0L
        } else {
            controller.duration
        }
        _state.value = PlayerState.MediaLoaded(
            isPlaying = controller.isPlaying,
            isLoading = controller.playbackState == Player.STATE_BUFFERING,
            showId = show.id,
            showTitle = show.title,
            durationInfo = MediaDurationInfo(
                currentPosition = controller.currentPosition,
                duration = duration
            ),
            artworkUri = metadata.artworkUri?.toString(),
            title = metadata.title?.toString() ?: "--",
            albumTitle = metadata.albumTitle?.toString() ?: "--",
            mediaId = cmi.mediaId,
            currentTrackIndex = controller.currentMediaItemIndex,
        )
    }

    private fun updateStateWithError(message: String) {
        val controller = mediaController ?: return
        val cmi = controller.currentMediaItem ?: return
        val show = cmi.showExtras ?: return
        val metadata = cmi.mediaMetadata
        _state.value = PlayerState.MediaLoaded.Error(
            playerError = PlayerError(message),
            showId = show.id,
            showTitle = show.title,
            durationInfo = MediaDurationInfo(0L, metadata.durationMs ?: 0L),
            artworkUri = metadata.artworkUri?.toString(),
            title = metadata.title?.toString() ?: "--",
            albumTitle = metadata.albumTitle?.toString() ?: "--",
            mediaId = cmi.mediaId,
            currentTrackIndex = controller.currentMediaItemIndex,
        )
    }

    override fun setPlaylist(items: List<PlaybackItem>, startIndex: Int) {
        val mediaItems = items.toMediaItems()
        val controller = mediaController ?: return
        controller.setMediaItems(mediaItems, startIndex, C.TIME_UNSET)
        controller.prepare()
        controller.play()
    }

    override fun play() { mediaController?.play() }
    override fun pause() { mediaController?.pause() }

    override fun seekTo(index: Int, positionMs: Long) {
        mediaController?.seekTo(index, positionMs)
    }

    override fun skipToPrevious() { mediaController?.seekToPreviousMediaItem() }
    override fun skipToNext() { mediaController?.seekToNextMediaItem() }

    override fun release() {
        scope.cancel()
        mediaController?.release()
        mediaController = null
    }
}
