package gizz.tapes.playback

import co.touchlab.kermit.Logger
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
import platform.Foundation.NSURL
import kotlin.time.Duration.Companion.seconds

/**
 * Drives local playback via AVQueuePlayer. Extracted from what used to be the entire
 * IosMediaPlayer so that class can route between this and CastPlaybackBackend while casting.
 * Now-playing info / remote-command-center wiring lives on the router (IosMediaPlayer) instead,
 * since there is exactly one system-level "now playing" surface regardless of which backend
 * is active.
 */
@OptIn(ExperimentalForeignApi::class)
class LocalPlaybackBackend(
    private val currentlyPlayingSaver: CurrentlyPlayingSaver,
) : PlaybackBackend {

    private val logger = Logger.withTag("LocalPlaybackBackend")

    private val player = AVQueuePlayer()
    private val _state = MutableStateFlow<PlayerState>(PlayerState.NoMedia)
    override val state: StateFlow<PlayerState> = _state.asStateFlow()
    override val currentPosition: Long
        get() = (CMTimeGetSeconds(player.currentTime()) * 1000.0).toLong()

    private var playlist: List<PlaybackItem> = emptyList()
    private var currentIndex = -1

    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    // local downloads are stored as plain filesystem paths - URLWithString requires a
    // percent-encoded URL string, which a raw path with spaces/colons is not, so route
    // local paths through fileURLWithPath instead.
    private fun PlaybackItem.toNSURL(): NSURL? =
        if (url.startsWith("/")) NSURL.fileURLWithPath(url) else NSURL.URLWithString(url)

    private var savingJob: Job? = null

    init {
        try {
            val session = AVAudioSession.sharedInstance()
            session.setCategory(AVAudioSessionCategoryPlayback, null)
            session.setActive(true, null)
        } catch (@Suppress("TooGenericExceptionCaught") e: Exception) {
            logger.e(e) { "Error setting up AVAudioSession" }
        }

        scope.launch {
            val stored = currentlyPlayingSaver.storedSession()
            if (stored.items.isNotEmpty()) {
                playlist = stored.items
                currentIndex = stored.currentTrack.coerceIn(0, (stored.items.size - 1).coerceAtLeast(0))
                stored.items.drop(currentIndex).forEach { item ->
                    item.toNSURL()?.let { url ->
                        player.insertItem(AVPlayerItem(url), afterItem = null)
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

    private fun updateState() {
        // Sync currentIndex with what AVQueuePlayer is actually playing — it auto-advances
        // through the queue when tracks finish, so we need to detect that here.
        // items() returns all remaining items including the current one, so the current
        // index is derived from how many items have been consumed from the playlist.
        val remainingItems = player.items()
        if (remainingItems.isNotEmpty()) {
            currentIndex = playlist.size - remainingItems.size
        } else if (playlist.isNotEmpty()) {
            // AVQueuePlayer has drained the queue - playback finished.
            playlist = emptyList()
            currentIndex = -1
            stopSaving()
        }
        val avItem = player.currentItem()

        val item = playlist.getOrNull(currentIndex)
        if (item == null) {
            _state.value = PlayerState.NoMedia
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
    }

    override fun setPlaylist(items: List<PlaybackItem>, startIndex: Int) {
        playlist = items
        currentIndex = startIndex.coerceIn(0, (items.size - 1).coerceAtLeast(0))
        player.removeAllItems()
        items.drop(currentIndex).forEach { item ->
            item.toNSURL()?.let { url ->
                player.insertItem(AVPlayerItem(url), afterItem = null)
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
            playlist.drop(currentIndex).forEach { item ->
                item.toNSURL()?.let { url ->
                    player.insertItem(AVPlayerItem(url), afterItem = null)
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
    }

    override fun snapshot(): PlaybackSnapshot = PlaybackSnapshot(
        items = playlist,
        currentIndex = currentIndex.coerceAtLeast(0),
        isPlaying = player.rate() != 0.0f,
        positionMs = currentPosition,
    )
}
