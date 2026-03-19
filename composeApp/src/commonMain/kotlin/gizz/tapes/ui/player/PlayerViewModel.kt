package gizz.tapes.ui.player

import androidx.lifecycle.ViewModel
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesIntoMap
import dev.zacsweers.metro.Inject
import dev.zacsweers.metrox.viewmodel.ViewModelKey
import gizz.tapes.playback.GizzMediaPlayer
import kotlinx.coroutines.flow.StateFlow

@Inject
@ContributesIntoMap(AppScope::class)
@ViewModelKey(PlayerViewModel::class)
class PlayerViewModel(
    private val mediaPlayer: GizzMediaPlayer,
) : ViewModel() {

    val playerState: StateFlow<PlayerState> = mediaPlayer.state

    fun play() = mediaPlayer.play()
    fun pause() = mediaPlayer.pause()
    fun seekTo(index: Int, positionMs: Long) = mediaPlayer.seekTo(index, positionMs)
    fun skipToPrevious() = mediaPlayer.skipToPrevious()
    fun skipToNext() = mediaPlayer.skipToNext()
}
