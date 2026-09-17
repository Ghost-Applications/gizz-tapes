package gizz.tapes.playback

import gizz.tapes.ui.player.PlayerState
import kotlinx.coroutines.flow.StateFlow

/**
 * A single playback engine - either the local AVQueuePlayer or an active Cast session.
 * IosMediaPlayer routes every GizzMediaPlayer call to whichever backend is currently active,
 * and uses [snapshot] to hand off playlist/position state when switching between them.
 */
interface PlaybackBackend {
    val state: StateFlow<PlayerState>
    val currentPosition: Long

    fun setPlaylist(items: List<PlaybackItem>, startIndex: Int = 0)
    fun play()
    fun pause()
    fun seekTo(index: Int, positionMs: Long)
    fun skipToPrevious()
    fun skipToNext()
    fun release()
    fun snapshot(): PlaybackSnapshot
}

data class PlaybackSnapshot(
    val items: List<PlaybackItem>,
    val currentIndex: Int,
    val isPlaying: Boolean,
    val positionMs: Long,
)
