package gizz.tapes.playback

import android.content.ComponentName
import android.content.Context
import androidx.annotation.OptIn
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.MoreExecutors
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

interface MediaPlayerContainer {
    val mediaPlayer: Player?
}

/**
 * Container for the MediaController basically used to get the
 * media controller into the dependency graph.
 */
@OptIn(UnstableApi::class)
@Singleton
class RealMediaPlayerContainer @Inject constructor(
    @ApplicationContext val context: Context,
): MediaPlayerContainer {

    private val controllerFuture: ListenableFuture<MediaController>
    private var _mediaController: MediaController? = null

    override val mediaPlayer: Player? get() =_mediaController

    init {
        val sessionToken = SessionToken(context, ComponentName(context, PlaybackService::class.java))
        val mediaControllerFuture = MediaController.Builder(context, sessionToken).buildAsync()
        controllerFuture = mediaControllerFuture

        mediaControllerFuture.addListener(
            {
                _mediaController = mediaControllerFuture.get()
            },
            MoreExecutors.directExecutor()
        )
    }
}
