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
import gizz.tapes.data.BAND_NAME
import gizz.tapes.ui.player.MediaDurationInfo
import gizz.tapes.ui.player.PlayerError
import gizz.tapes.ui.player.PlayerState
import gizz.tapes.util.getRemoteUrl
import gizz.tapes.util.resolveShowDestination
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
import kotlin.time.Duration.Companion.milliseconds

@Inject
@ContributesBinding(AppScope::class)
@SingleIn(AppScope::class)
class AndroidMediaPlayer(
    context: Context,
    errorReporter: PlaybackErrorReporter,
) : GizzMediaPlayer {

    private val _state = MutableStateFlow<PlayerState>(PlayerState.NoMedia)
    override val state: StateFlow<PlayerState> = _state.asStateFlow()

    override val currentPosition: Long get() = mediaController?.currentPosition ?: 0L

    @Volatile private var mediaController: MediaController? = null
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var pollingJob: Job? = null

    // Reported errors (e.g. a Cast receiver failing a track) aren't sticky like ExoPlayer errors -
    // the receiver moves straight on to the next track - so hold the error until something plays
    // again, otherwise the next state update replaces it before the UI can show it.
    private var heldError: PlayerState.MediaLoaded.Error? = null

    // A reported error can arrive while there's briefly no current item (e.g. mid-handoff to a
    // Cast device) - keep it until a state update can attach it to a track.
    private var pendingErrorMessage: String? = null

    init {
        scope.launch {
            errorReporter.errors.collect { message ->
                pendingErrorMessage = message
                updateState()
            }
        }
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
            updateState()

            controller.addListener(object : Player.Listener {
                override fun onIsPlayingChanged(isPlaying: Boolean) {
                    updateState()
                    if (isPlaying) {
                        startPolling()
                    } else {
                        stopPolling()
                    }
                }

                override fun onPlaybackStateChanged(state: Int) {
                    updateState()
                    if (state == Player.STATE_ENDED) {
                        controller.clearMediaItems()
                    }
                }

                override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) =
                    updateState()

                override fun onPlayerError(error: PlaybackException) {
                    val item = controller.currentMediaItem
                    val message = if (
                        error.errorCode == PlaybackException.ERROR_CODE_IO_BAD_HTTP_STATUS &&
                        isArchiveOrgUrl(item?.mediaMetadata?.extras?.getRemoteUrl())
                    ) {
                        archiveOrgErrorMessage(item?.mediaMetadata?.title?.toString())
                    } else {
                        error.localizedMessage ?: "Playback error"
                    }
                    updateStateWithError(message)
                }
            })
        }, ContextCompat.getMainExecutor(appContext))
    }

    private fun startPolling() {
        pollingJob?.cancel()
        pollingJob = scope.launch {
            while (true) {
                delay(700.milliseconds)
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
        if (showReportedError(controller)) return
        val cmi = controller.currentMediaItem
        if (cmi == null || cmi.mediaId.isEmpty()) {
            _state.value = PlayerState.NoMedia
            return
        }
        val show = cmi.resolveShowDestination() ?: run {
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
            artistName = metadata.artist?.toString() ?: BAND_NAME,
        )
    }

    // Returns true if a reported error was shown, in place of the normal state.
    private fun showReportedError(controller: MediaController): Boolean {
        pendingErrorMessage?.let { message ->
            if (updateStateWithError(message)) {
                pendingErrorMessage = null
                heldError = _state.value as? PlayerState.MediaLoaded.Error
                return true
            }
        }
        val error = heldError ?: return false
        if (controller.isPlaying) {
            heldError = null
            return false
        }
        _state.value = error
        return true
    }

    private fun updateStateWithError(message: String): Boolean {
        val controller = mediaController ?: return false
        val cmi = controller.currentMediaItem ?: return false
        val show = cmi.resolveShowDestination() ?: return false
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
        return true
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
