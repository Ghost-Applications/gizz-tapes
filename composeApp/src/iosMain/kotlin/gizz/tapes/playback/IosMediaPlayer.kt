package gizz.tapes.playback

import co.touchlab.kermit.Logger
import cocoapods.google_cast_sdk.GCKCastSession
import cocoapods.google_cast_sdk.GCKSession
import cocoapods.google_cast_sdk.GCKSessionManager
import cocoapods.google_cast_sdk.GCKSessionManagerListenerProtocol
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import dev.zacsweers.metro.binding
import gizz.tapes.ui.player.PlayerState
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import platform.Foundation.NSData
import platform.Foundation.NSError
import platform.Foundation.NSURL
import platform.Foundation.dataWithContentsOfURL
import platform.MediaPlayer.MPChangePlaybackPositionCommandEvent
import platform.MediaPlayer.MPMediaItemArtwork
import platform.MediaPlayer.MPMediaItemPropertyAlbumTitle
import platform.MediaPlayer.MPMediaItemPropertyArtwork
import platform.MediaPlayer.MPMediaItemPropertyPlaybackDuration
import platform.MediaPlayer.MPMediaItemPropertyTitle
import platform.MediaPlayer.MPNowPlayingInfoCenter
import platform.MediaPlayer.MPNowPlayingInfoPropertyElapsedPlaybackTime
import platform.MediaPlayer.MPNowPlayingInfoPropertyPlaybackRate
import platform.MediaPlayer.MPNowPlayingPlaybackStatePaused
import platform.MediaPlayer.MPNowPlayingPlaybackStatePlaying
import platform.MediaPlayer.MPNowPlayingPlaybackStateStopped
import platform.MediaPlayer.MPRemoteCommandCenter
import platform.MediaPlayer.MPRemoteCommandHandlerStatusSuccess
import platform.UIKit.UIImage
import platform.darwin.NSObject

/**
 * Cross-platform GizzMediaPlayer implementation for iOS. Routes every call to whichever
 * [PlaybackBackend] is currently active - the local AVQueuePlayer-backed one normally, or a
 * Cast-backed one while a Chromecast session is connected - so ViewModels see a single stable
 * player regardless of where audio is actually playing. Also owns the system-level "now playing"
 * surface (MPNowPlayingInfoCenter/MPRemoteCommandCenter), since there is exactly one of those
 * regardless of which backend is active.
 */
@Inject
@ContributesBinding(AppScope::class, binding = binding<GizzMediaPlayer>())
@SingleIn(AppScope::class)
@OptIn(ExperimentalForeignApi::class)
class IosMediaPlayer(
    private val currentlyPlayingSaver: CurrentlyPlayingSaver,
) : GizzMediaPlayer {

    private val logger = Logger.withTag("IosMediaPlayer")

    private val localBackend = LocalPlaybackBackend(currentlyPlayingSaver)
    private var castBackend: CastPlaybackBackend? = null
    private var active: PlaybackBackend = localBackend

    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var stateForwardingJob: Job? = null

    private val _state = MutableStateFlow<PlayerState>(PlayerState.NoMedia)
    override val state: StateFlow<PlayerState> = _state.asStateFlow()
    override val currentPosition: Long
        get() = active.currentPosition

    private var lastArtworkUrl: String? = null
    private var cachedArtwork: MPMediaItemArtwork? = null

    private val sessionListener = SessionManagerListener(
        onSessionStarted = ::switchToCast,
        onSessionEnded = ::switchToLocal,
        onSessionFailedToStart = { error ->
            logger.e { "Cast session failed to start: ${error.localizedDescription}" }
        },
    )

    init {
        setupRemoteCommands()
        forwardState(localBackend)
        GizzCastContext.sessionManager.addListener(sessionListener)
        GizzCastContext.sessionManager.currentCastSession?.let(::switchToCast)
    }

    private fun forwardState(backend: PlaybackBackend) {
        stateForwardingJob?.cancel()
        stateForwardingJob = backend.state
            .onEach { state ->
                _state.value = state
                updateNowPlayingInfo(state)
            }
            .launchIn(scope)
    }

    private fun switchToCast(session: GCKCastSession) {
        if (castBackend != null) return
        val snapshot = active.snapshot()
        localBackend.pause()

        val backend = CastPlaybackBackend().apply { attach(session) }
        castBackend = backend
        active = backend
        forwardState(backend)

        if (snapshot.items.isNotEmpty()) {
            backend.setPlaylist(snapshot.items, snapshot.currentIndex)
            backend.seekTo(snapshot.currentIndex, snapshot.positionMs)
            if (snapshot.isPlaying) backend.play() else backend.pause()
        }
    }

    private fun switchToLocal() {
        val backend = castBackend ?: return
        val snapshot = backend.snapshot()
        backend.release()
        castBackend = null
        active = localBackend
        forwardState(localBackend)

        if (snapshot.items.isNotEmpty()) {
            localBackend.setPlaylist(snapshot.items, snapshot.currentIndex)
            localBackend.seekTo(snapshot.currentIndex, snapshot.positionMs)
            if (snapshot.isPlaying) localBackend.play() else localBackend.pause()
        }
    }

    private fun setupRemoteCommands() {
        val cc = MPRemoteCommandCenter.sharedCommandCenter()

        // Dispatch through scope so playback is always driven from the main thread, regardless
        // of which thread the system uses to invoke these handlers.
        cc.playCommand.addTargetWithHandler { _ -> scope.launch { play() }; MPRemoteCommandHandlerStatusSuccess }
        cc.pauseCommand.addTargetWithHandler { _ -> scope.launch { pause() }; MPRemoteCommandHandlerStatusSuccess }
        cc.nextTrackCommand.addTargetWithHandler { _ -> scope.launch { skipToNext() }; MPRemoteCommandHandlerStatusSuccess }
        cc.previousTrackCommand.addTargetWithHandler { _ -> scope.launch { skipToPrevious() }; MPRemoteCommandHandlerStatusSuccess }
        // Some headphone remotes (wired single-button controls, Bluetooth AVRCP) send a single
        // toggle command instead of distinct play/pause - handle it explicitly rather than
        // relying on the system to synthesize a play/pause call.
        cc.togglePlayPauseCommand.addTargetWithHandler { _ ->
            val isPlaying = (_state.value as? PlayerState.MediaLoaded)?.isPlaying == true
            scope.launch { if (isPlaying) pause() else play() }
            MPRemoteCommandHandlerStatusSuccess
        }
        cc.changePlaybackPositionCommand.addTargetWithHandler { event ->
            val positionSeconds = (event as MPChangePlaybackPositionCommandEvent).positionTime
            val index = (_state.value as? PlayerState.MediaLoaded)?.currentTrackIndex ?: 0
            scope.launch { seekTo(index, (positionSeconds * 1000.0).toLong()) }
            MPRemoteCommandHandlerStatusSuccess
        }
    }

    private fun updateNowPlayingInfo(state: PlayerState) {
        val item = state as? PlayerState.MediaLoaded
        if (item == null) {
            MPNowPlayingInfoCenter.defaultCenter().nowPlayingInfo = null
            return
        }

        val info = mutableMapOf<Any?, Any?>()
        info[MPMediaItemPropertyTitle] = item.title
        info[MPMediaItemPropertyAlbumTitle] = item.albumTitle
        info[MPMediaItemPropertyPlaybackDuration] = item.durationInfo.duration / 1000.0
        info[MPNowPlayingInfoPropertyElapsedPlaybackTime] = item.durationInfo.currentPosition / 1000.0
        info[MPNowPlayingInfoPropertyPlaybackRate] = if (item.isPlaying) 1.0 else 0.0

        cachedArtwork?.let { info[MPMediaItemPropertyArtwork] = it }

        MPNowPlayingInfoCenter.defaultCenter().nowPlayingInfo = info
        // Must be kept in sync with actual playback state, otherwise iOS can lose track of
        // which app owns remote-control focus and route the next headphone command elsewhere.
        MPNowPlayingInfoCenter.defaultCenter().playbackState =
            if (item.isPlaying) MPNowPlayingPlaybackStatePlaying else MPNowPlayingPlaybackStatePaused

        val artworkUrl = item.artworkUri
        if (artworkUrl != null && artworkUrl != lastArtworkUrl) {
            lastArtworkUrl = artworkUrl
            cachedArtwork = null
            scope.launch(Dispatchers.IO) {
                loadArtwork(artworkUrl)?.let { artwork ->
                    cachedArtwork = artwork
                    val updatedInfo = MPNowPlayingInfoCenter.defaultCenter().nowPlayingInfo
                        ?.toMutableMap() ?: mutableMapOf()
                    updatedInfo[MPMediaItemPropertyArtwork] = artwork
                    withContext(Dispatchers.Main) {
                        MPNowPlayingInfoCenter.defaultCenter().nowPlayingInfo = updatedInfo
                    }
                }
            }
        }
    }

    private fun loadArtwork(urlString: String): MPMediaItemArtwork? {
        val url = NSURL.URLWithString(urlString) ?: return null
        val data = NSData.dataWithContentsOfURL(url) ?: return null
        val image = UIImage.imageWithData(data) ?: return null
        return MPMediaItemArtwork(boundsSize = image.size) { _ -> image }
    }

    override fun setPlaylist(items: List<PlaybackItem>, startIndex: Int) = active.setPlaylist(items, startIndex)
    override fun play() = active.play()
    override fun pause() = active.pause()
    override fun seekTo(index: Int, positionMs: Long) = active.seekTo(index, positionMs)
    override fun skipToPrevious() = active.skipToPrevious()
    override fun skipToNext() = active.skipToNext()

    override fun release() {
        castBackend?.release()
        localBackend.release()
        GizzCastContext.sessionManager.removeListener(sessionListener)
        MPNowPlayingInfoCenter.defaultCenter().nowPlayingInfo = null
        MPNowPlayingInfoCenter.defaultCenter().playbackState = MPNowPlayingPlaybackStateStopped
        val cc = MPRemoteCommandCenter.sharedCommandCenter()
        cc.playCommand.enabled = false
        cc.pauseCommand.enabled = false
        cc.togglePlayPauseCommand.enabled = false
        cc.nextTrackCommand.enabled = false
        cc.previousTrackCommand.enabled = false
        cc.changePlaybackPositionCommand.enabled = false
        scope.cancel()
    }
}

/**
 * Forwards Cast session lifecycle events to IosMediaPlayer via plain closures. Kotlin/Native
 * doesn't support mixing GCKSessionManagerListenerProtocol with a plain Kotlin interface
 * (GizzMediaPlayer) on one class - same pattern as CastPlaybackBackend's MediaStatusListener.
 */
@OptIn(ExperimentalForeignApi::class)
private class SessionManagerListener(
    private val onSessionStarted: (GCKCastSession) -> Unit,
    private val onSessionEnded: () -> Unit,
    private val onSessionFailedToStart: (NSError) -> Unit,
) : NSObject(), GCKSessionManagerListenerProtocol {

    @kotlinx.cinterop.ObjCSignatureOverride
    override fun sessionManager(sessionManager: GCKSessionManager, didStartSession: GCKSession) {
        (didStartSession as? GCKCastSession)?.let(onSessionStarted)
    }

    @kotlinx.cinterop.ObjCSignatureOverride
    override fun sessionManager(sessionManager: GCKSessionManager, didResumeSession: GCKSession) {
        (didResumeSession as? GCKCastSession)?.let(onSessionStarted)
    }

    override fun sessionManager(sessionManager: GCKSessionManager, didEndSession: GCKSession, withError: NSError?) =
        onSessionEnded()

    override fun sessionManager(
        sessionManager: GCKSessionManager,
        didFailToStartSession: GCKSession,
        withError: NSError,
    ) = onSessionFailedToStart(withError)
}
