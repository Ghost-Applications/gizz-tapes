package gizz.tapes.playback

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
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
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

@Inject
@ContributesBinding(AppScope::class)
@SingleIn(AppScope::class)
@OptIn(ExperimentalForeignApi::class)
class IosMediaPlayer : GizzMediaPlayer {

    private val player = AVQueuePlayer()
    private val _state = MutableStateFlow<PlayerState>(PlayerState.NoMedia)
    override val state: StateFlow<PlayerState> = _state.asStateFlow()
    override val currentPosition: Long
        get() = (CMTimeGetSeconds(player.currentTime()) * 1000.0).toLong()

    private var playlist: List<PlaybackItem> = emptyList()
    private var currentIndex = -1
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    init {
        try {
            val session = AVAudioSession.sharedInstance()
            session.setCategory(AVAudioSessionCategoryPlayback, null)
            session.setActive(true, null)
        } catch (_: Exception) {}

        scope.launch {
            while (true) {
                delay(500)
                updateState()
            }
        }
    }

    private fun updateState() {
        val item = playlist.getOrNull(currentIndex)
        if (item == null) {
            _state.value = PlayerState.NoMedia
            return
        }

        val positionMs = currentPosition
        val avItem = player.currentItem()
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
        _state.value = PlayerState.MediaLoaded(
            isPlaying = player.rate() != 0.0f,
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
            val url = NSURL.URLWithString(item.url) ?: return@forEach
            player.insertItem(AVPlayerItem(url), afterItem = null)
        }
        updateState()
    }

    override fun play() {
        player.play()
        updateState()
    }

    override fun pause() {
        player.pause()
        updateState()
    }

    override fun seekTo(index: Int, positionMs: Long) {
        if (index != currentIndex) {
            setPlaylist(playlist, index)
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
        scope.cancel()
        player.pause()
        player.removeAllItems()
    }
}
