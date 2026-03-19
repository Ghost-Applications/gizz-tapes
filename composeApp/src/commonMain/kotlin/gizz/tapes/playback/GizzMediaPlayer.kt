package gizz.tapes.playback

import gizz.tapes.data.FullShowTitle
import gizz.tapes.data.ShowId
import gizz.tapes.ui.player.PlayerState
import kotlinx.coroutines.flow.StateFlow
import kotlinx.datetime.LocalDate
import kotlinx.serialization.Serializable

/**
 * Cross-platform media player interface.
 * Platform-specific implementations:
 *  - Android: ExoPlayer (media3)
 *  - iOS: AVQueuePlayer (AVFoundation)
 *  - Desktop: VLC (vlcj) — requires VLC to be installed
 */
interface GizzMediaPlayer {
    val state: StateFlow<PlayerState>
    val currentPosition: Long

    fun setPlaylist(items: List<PlaybackItem>, startIndex: Int = 0)
    fun play()
    fun pause()
    fun seekTo(index: Int, positionMs: Long)
    fun skipToPrevious()
    fun skipToNext()
    fun release()
}

@Serializable
data class PlaybackItem(
    val id: String,
    val url: String,
    val title: String,
    val albumTitle: String,
    val artworkUrl: String?,
    val showId: ShowId,
    val showTitle: FullShowTitle,
    val durationMs: Long,
    val showDate: LocalDate,
)
