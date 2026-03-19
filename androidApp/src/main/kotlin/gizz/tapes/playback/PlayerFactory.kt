package gizz.tapes.playback

import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer

@UnstableApi
interface PlayerFactory {
    fun create(exoPlayer: ExoPlayer): Player
}
