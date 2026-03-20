package gizz.tapes.playback

import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject

@Inject
@UnstableApi
@ContributesBinding(AppScope::class)
class FossPlayerFactory : PlayerFactory {
    override fun create(exoPlayer: ExoPlayer): Player = exoPlayer
}
