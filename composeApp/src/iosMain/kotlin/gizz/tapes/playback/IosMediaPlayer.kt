package gizz.tapes.playback

import co.touchlab.kermit.Logger
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import gizz.tapes.ui.player.MediaDurationInfo
import gizz.tapes.ui.player.PlayerError
import gizz.tapes.ui.player.PlayerState
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import platform.AVFAudio.AVAudioSession
import platform.AVFAudio.AVAudioSessionCategoryPlayback
import platform.AVFAudio.setActive
import platform.AVFoundation.AVPlayerItem
import platform.AVFoundation.AVPlayerItemStatusFailed
import platform.AVFoundation.AVPlayerItemStatusUnknown
import platform.AVFoundation.AVQueuePlayer
import platform.AVFoundation.currentItem
import platform.AVFoundation.currentTime
import platform.AVFoundation.duration
import platform.AVFoundation.pause
import platform.AVFoundation.play
import platform.AVFoundation.rate
import platform.AVFoundation.seekToTime
import platform.CoreMedia.CMTimeGetSeconds
import platform.CoreMedia.CMTimeMake
import platform.Foundation.NSData
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
import platform.MediaPlayer.MPRemoteCommandCenter
import platform.MediaPlayer.MPRemoteCommandHandlerStatusSuccess
import platform.UIKit.UIImage
import kotlin.time.Duration.Companion.seconds

@Inject
@ContributesBinding(AppScope::class)
@SingleIn(AppScope::class)
@OptIn(ExperimentalForeignApi::class)
class IosMediaPlayer(
    private val currentlyPlayingSaver: CurrentlyPlayingSaver,
) : GizzMediaPlayer {

    private val logger = Logger.withTag("IosMediaPlayer")

    private val player = AVQueuePlayer()
    private val _state = MutableStateFlow<PlayerState>(PlayerState.NoMedia)
    override val state: StateFlow<PlayerState> = _state.asStateFlow()
    override val currentPosition: Long
        get() = (CMTimeGetSeconds(player.currentTime()) * 1000.0).toLong()

    private var playlist: List<PlaybackItem> = emptyList()
    private var currentIndex = -1

    // Tracks AVPlayerItem references so we can detect when the queue auto-advances.
    // Always corresponds to playlist[queueStartIndex..end].
    private var playerItems: List<AVPlayerItem> = emptyList()
    private var queueStartIndex: Int = 0
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    private var lastArtworkUrl: String? = null
    private var cachedArtwork: MPMediaItemArtwork? = null
    private var savingJob: Job? = null

    init {
        try {
            val session = AVAudioSession.sharedInstance()
            session.setCategory(AVAudioSessionCategoryPlayback, null)
            session.setActive(true, null)
        } catch (@Suppress("TooGenericExceptionCaught") e: Exception) {
            logger.e(e) { "Error setting up AVAudioSession" }
        }

        setupRemoteCommands()

        scope.launch {
            val stored = currentlyPlayingSaver.storedSession()
            if (stored.items.isNotEmpty()) {
                playlist = stored.items
                currentIndex = stored.currentTrack.coerceIn(0, (stored.items.size - 1).coerceAtLeast(0))
                queueStartIndex = currentIndex
                playerItems = stored.items.drop(currentIndex).mapNotNull { item ->
                    NSURL.URLWithString(item.url)?.let { url ->
                        AVPlayerItem(url).also { player.insertItem(it, afterItem = null) }
                    }
                }
                val cmTime = CMTimeMake(stored.currentTime, 1000)
                player.seekToTime(cmTime)
                updateState()
            }
        }

        scope.launch {
            while (true) {
                delay(500)
                updateState()
            }
        }
    }

    private fun startSaving() {
        savingJob?.cancel()
        savingJob = scope.launch {
            while (isActive) {
                delay(5.seconds)
                saveState()
            }
        }
    }

    private fun stopSaving() {
        savingJob?.cancel()
        savingJob = null
        scope.launch { saveState() }
    }

    private suspend fun saveState() {
        val position = currentPosition
        val items = playlist
        val index = currentIndex
        withContext(Dispatchers.IO) {
            currentlyPlayingSaver.save(
                items = items,
                currentTrackIndex = index,
                currentPosition = position,
            )
        }
    }

    private fun setupRemoteCommands() {
        val cc = MPRemoteCommandCenter.sharedCommandCenter()

        // Dispatch through scope so AVQueuePlayer is always accessed on the main thread,
        // regardless of which thread the system uses to invoke these handlers.
        cc.playCommand.addTargetWithHandler { _ -> scope.launch { play() }; MPRemoteCommandHandlerStatusSuccess }
        cc.pauseCommand.addTargetWithHandler { _ -> scope.launch { pause() }; MPRemoteCommandHandlerStatusSuccess }
        cc.nextTrackCommand.addTargetWithHandler { _ -> scope.launch { skipToNext() }; MPRemoteCommandHandlerStatusSuccess }
        cc.previousTrackCommand.addTargetWithHandler { _ -> scope.launch { skipToPrevious() }; MPRemoteCommandHandlerStatusSuccess }
        cc.changePlaybackPositionCommand.addTargetWithHandler { event ->
            val positionSeconds = (event as MPChangePlaybackPositionCommandEvent).positionTime
            scope.launch { seekTo(currentIndex, (positionSeconds * 1000.0).toLong()) }
            MPRemoteCommandHandlerStatusSuccess
        }
    }

    private fun updateNowPlayingInfo(item: PlaybackItem, isPlaying: Boolean) {
        val info = mutableMapOf<Any?, Any?>()
        info[MPMediaItemPropertyTitle] = item.title
        info[MPMediaItemPropertyAlbumTitle] = item.albumTitle
        info[MPMediaItemPropertyPlaybackDuration] = item.durationMs / 1000.0
        info[MPNowPlayingInfoPropertyElapsedPlaybackTime] = currentPosition / 1000.0
        info[MPNowPlayingInfoPropertyPlaybackRate] = if (isPlaying) 1.0 else 0.0

        cachedArtwork?.let { info[MPMediaItemPropertyArtwork] = it }

        MPNowPlayingInfoCenter.defaultCenter().nowPlayingInfo = info

        val artworkUrl = item.artworkUrl
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

    private fun updateState() {
        // Sync currentIndex with what AVQueuePlayer is actually playing — it auto-advances
        // through the queue when tracks finish, so we need to detect that here.
        // playerItems[i] corresponds to playlist[queueStartIndex + i].
        val avItem = player.currentItem()
        val playerItemIndex = playerItems.indexOfFirst { it === avItem }
        if (playerItemIndex != -1) {
            currentIndex = queueStartIndex + playerItemIndex
        }

        val item = playlist.getOrNull(currentIndex)
        if (item == null) {
            _state.value = PlayerState.NoMedia
            MPNowPlayingInfoCenter.defaultCenter().nowPlayingInfo = null
            return
        }

        val positionMs = currentPosition
        val durationSeconds = avItem?.let { CMTimeGetSeconds(it.duration()) }
        val durationMs = if (durationSeconds != null && !durationSeconds.isNaN() && durationSeconds > 0.0) {
            (durationSeconds * 1000.0).toLong()
        } else {
            item.durationMs
        }

        val currentStatus = avItem?.status()
        val isError = currentStatus == AVPlayerItemStatusFailed

        if (isError) {
            _state.value = PlayerState.MediaLoaded.Error(
                playerError = PlayerError("Playback failed"),
                showId = item.showId,
                showTitle = item.showTitle,
                durationInfo = MediaDurationInfo(positionMs, durationMs),
                artworkUri = item.artworkUrl,
                title = item.title,
                albumTitle = item.albumTitle,
                mediaId = item.id,
                currentTrackIndex = currentIndex,
            )
            return
        }

        val isLoading = currentStatus == AVPlayerItemStatusUnknown
        val isPlaying = player.rate() != 0.0f
        _state.value = PlayerState.MediaLoaded(
            isPlaying = isPlaying,
            isLoading = isLoading,
            showId = item.showId,
            showTitle = item.showTitle,
            durationInfo = MediaDurationInfo(positionMs, durationMs),
            artworkUri = item.artworkUrl,
            title = item.title,
            albumTitle = item.albumTitle,
            mediaId = item.id,
            currentTrackIndex = currentIndex,
        )

        updateNowPlayingInfo(item, isPlaying)
    }

    override fun setPlaylist(items: List<PlaybackItem>, startIndex: Int) {
        playlist = items
        currentIndex = startIndex.coerceIn(0, (items.size - 1).coerceAtLeast(0))
        player.removeAllItems()
        queueStartIndex = currentIndex
        playerItems = items.drop(currentIndex).mapNotNull { item ->
            NSURL.URLWithString(item.url)?.let { url ->
                AVPlayerItem(url).also { player.insertItem(it, afterItem = null) }
            }
        }
        player.play()
        startSaving()
        updateState()
    }

    override fun play() {
        player.play()
        startSaving()
        updateState()
    }

    override fun pause() {
        player.pause()
        stopSaving()
        updateState()
    }

    override fun seekTo(index: Int, positionMs: Long) {
        if (index != currentIndex) {
            // Remember whether we were playing so we can resume after rebuilding the queue
            val wasPlaying = player.rate() != 0.0f

            // Clamp index to valid range in case caller passes out-of-bounds value
            currentIndex = index.coerceIn(0, (playlist.size - 1).coerceAtLeast(0))

            // AVQueuePlayer has no way to jump to an arbitrary position in the queue,
            // so we rebuild it starting from the new index
            player.removeAllItems()
            queueStartIndex = currentIndex
            playerItems = playlist.drop(currentIndex).mapNotNull { item ->
                NSURL.URLWithString(item.url)?.let { url ->
                    AVPlayerItem(url).also { player.insertItem(it, afterItem = null) }
                }
            }

            // Resume playback only if we were already playing — don't force play on a seek
            if (wasPlaying) player.play()
        }
        val cmTime = CMTimeMake(positionMs, 1000)
        player.seekToTime(cmTime)
        updateState()
    }

    override fun skipToPrevious() {
        if (currentIndex > 0) seekTo(currentIndex - 1, 0L)
    }

    override fun skipToNext() {
        if (currentIndex < playlist.size - 1) seekTo(currentIndex + 1, 0L)
    }

    override fun release() {
        savingJob?.cancel()
        scope.cancel()
        player.pause()
        player.removeAllItems()
        MPNowPlayingInfoCenter.defaultCenter().nowPlayingInfo = null
        val cc = MPRemoteCommandCenter.sharedCommandCenter()
        cc.playCommand.enabled = false
        cc.pauseCommand.enabled = false
        cc.nextTrackCommand.enabled = false
        cc.previousTrackCommand.enabled = false
        cc.changePlaybackPositionCommand.enabled = false
    }
}
