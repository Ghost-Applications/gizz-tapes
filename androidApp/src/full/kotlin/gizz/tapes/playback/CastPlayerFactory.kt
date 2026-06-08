package gizz.tapes.playback

import android.content.Context
import androidx.media3.cast.CastPlayer
import androidx.media3.cast.RemoteCastPlayer
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import co.touchlab.kermit.Logger
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject

@Inject
@UnstableApi
@ContributesBinding(AppScope::class)
class CastPlayerFactory(
    private val context: Context
) : PlayerFactory {
    override fun create(exoPlayer: ExoPlayer): Player {
        return runCatching {
            val remotePlayer = RemoteCastPlayer.Builder(context)
                .setMediaItemConverter(GizzMediaItemConverter())
                .build()
            CastPlayer.Builder(context)
                .setLocalPlayer(exoPlayer)
                .setRemotePlayer(remotePlayer)
                .build()
        }.getOrElse {
            Logger.e("Error creating CastPlayer, falling back to ExoPlayer", it)
            exoPlayer
        }
    }
}
