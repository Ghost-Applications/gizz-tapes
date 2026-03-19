package gizz.tapes.playback

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import gizz.tapes.ui.player.MediaDurationInfo
import gizz.tapes.ui.player.PlayerError
import gizz.tapes.ui.player.PlayerState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import uk.co.caprica.vlcj.factory.discovery.NativeDiscovery
import uk.co.caprica.vlcj.player.base.MediaPlayerEventAdapter
import uk.co.caprica.vlcj.player.component.AudioPlayerComponent
import uk.co.caprica.vlcj.player.base.MediaPlayer as VlcMediaPlayerBase

/**
 * Desktop media player using VLC via vlcj.
 * Requires VLC to be installed on the system.
 */
@Inject
@ContributesBinding(AppScope::class)
@SingleIn(AppScope::class)
class DesktopMediaPlayer : GizzMediaPlayer {

    private val vlcAvailable = NativeDiscovery().discover()
    private val playerComponent: AudioPlayerComponent? = if (vlcAvailable) {
        try { AudioPlayerComponent() } catch (_: Exception) { null }
    } else null
    private val vlcPlayer = playerComponent?.mediaPlayer()

    private val _state = MutableStateFlow<PlayerState>(PlayerState.NoMedia)
    override val state: StateFlow<PlayerState> = _state.asStateFlow()
    override val currentPosition: Long get() = vlcPlayer?.status()?.time() ?: 0L

    private var playlist: List<PlaybackItem> = emptyList()
    private var currentIndex = -1
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    init {
        vlcPlayer?.events()?.addMediaPlayerEventListener(object : MediaPlayerEventAdapter() {
            override fun playing(mp: VlcMediaPlayerBase) { updateState() }
            override fun paused(mp: VlcMediaPlayerBase) { updateState() }
            override fun stopped(mp: VlcMediaPlayerBase) { updateState() }
            override fun finished(mp: VlcMediaPlayerBase) {
                scope.launch(Dispatchers.Main) {
                    if (currentIndex < playlist.size - 1) skipToNext()
                }
            }
            override fun error(mp: VlcMediaPlayerBase) {
                updateStateWithError("Playback error")
            }
        })

        scope.launch {
            while (true) {
                delay(500)
                if (vlcPlayer?.status()?.isPlaying == true) updateState()
            }
        }
    }

    private fun updateState() {
        val item = playlist.getOrNull(currentIndex)
        if (item == null) {
            _state.value = PlayerState.NoMedia
            return
        }
        val positionMs = vlcPlayer?.status()?.time() ?: 0L
        val durationMs = vlcPlayer?.status()?.length()?.takeIf { it > 0L } ?: item.durationMs

        _state.value = PlayerState.MediaLoaded(
            isPlaying = vlcPlayer?.status()?.isPlaying == true,
            isLoading = false,
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

    private fun updateStateWithError(message: String) {
        val item = playlist.getOrNull(currentIndex) ?: return
        _state.value = PlayerState.MediaLoaded.Error(
            playerError = PlayerError(message),
            showId = item.showId,
            showTitle = item.showTitle,
            durationInfo = MediaDurationInfo(0L, item.durationMs),
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
        val item = items.getOrNull(currentIndex) ?: return
        vlcPlayer?.media()?.play(item.url)
        updateState()
    }

    override fun play() {
        vlcPlayer?.controls()?.play()
        updateState()
    }

    override fun pause() {
        vlcPlayer?.controls()?.pause()
        updateState()
    }

    override fun seekTo(index: Int, positionMs: Long) {
        if (index != currentIndex && index >= 0 && index < playlist.size) {
            currentIndex = index
            val item = playlist[index]
            vlcPlayer?.media()?.play(item.url)
            if (positionMs > 0L) {
                scope.launch {
                    delay(500) // wait for media to load
                    vlcPlayer?.controls()?.setTime(positionMs)
                }
            }
        } else {
            vlcPlayer?.controls()?.setTime(positionMs)
        }
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
        vlcPlayer?.controls()?.stop()
        playerComponent?.release()
    }
}
