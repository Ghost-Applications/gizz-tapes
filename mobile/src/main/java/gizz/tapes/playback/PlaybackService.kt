@file:kotlin.OptIn(ExperimentalMaterial3Api::class)

package gizz.tapes.playback

import android.app.PendingIntent
import android.app.PendingIntent.FLAG_IMMUTABLE
import android.app.PendingIntent.FLAG_UPDATE_CURRENT
import android.content.Intent
import androidx.annotation.OptIn
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.media3.cast.CastPlayer
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.LibraryResult
import androidx.media3.session.MediaLibraryService
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSession.MediaItemsWithStartPosition
import androidx.media3.session.SessionError
import arrow.core.getOrElse
import arrow.core.toOption
import arrow.fx.coroutines.parMap
import com.google.android.gms.cast.framework.CastContext
import com.google.common.collect.ImmutableList
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.MoreExecutors
import dagger.hilt.android.AndroidEntryPoint
import gizz.tapes.MainActivity
import gizz.tapes.data.MediaId
import gizz.tapes.util.CastAvailabilityChecker
import gizz.tapes.util.MediaItemWrapper
import gizz.tapes.util.MediaItemsWrapper
import gizz.tapes.util.realMediaId
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.guava.future
import timber.log.Timber
import javax.inject.Inject

@UnstableApi
@AndroidEntryPoint
class PlaybackService : MediaLibraryService(),
    MediaLibraryService.MediaLibrarySession.Callback {

    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private var mediaSession: MediaLibrarySession? = null
    private var exoPlayer: Player? = null
    private var castPlayer: Player? = null

    @Inject lateinit var replaceableForwardingPlayer: ReplaceableForwardingPlayerFactory
    @Inject lateinit var mediaItemTree: MediaItemTree

    lateinit var player: ReplaceableForwardingPlayer

    @OptIn(UnstableApi::class)
    override fun onCreate() {
        Timber.d("onCreate()")
        super.onCreate()

        val exoPlayer = ExoPlayer.Builder(this)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(C.USAGE_MEDIA)
                    .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                    .build(),
                true
            )
            .setHandleAudioBecomingNoisy(true)
            .setSkipSilenceEnabled(true)
            .build()

        if (CastAvailabilityChecker.isAvailable) { setupCastPlayer() }

        this.player = replaceableForwardingPlayer.create(exoPlayer)

        val pendingIntent = PendingIntent.getActivity(
            this,
            1337,
            Intent(this, MainActivity::class.java),
            FLAG_IMMUTABLE or FLAG_UPDATE_CURRENT
        )

        mediaSession = MediaLibrarySession.Builder(this, player, this)
            .setSessionActivity(pendingIntent)
            .build()
        this.exoPlayer = exoPlayer
    }

    override fun onDestroy() {
        Timber.d("onDestroy()")
        mediaSession?.run {
            player.release()
            release()
            mediaSession = null
            serviceScope.cancel()
        }
        super.onDestroy()
    }

    override fun onGetSession(
        controllerInfo: MediaSession.ControllerInfo
    ): MediaLibrarySession? = mediaSession

    // User dismissed the app from recent tasks
    override fun onTaskRemoved(rootIntent: Intent?) {
        Timber.d("onTaskRemoved() rootIntent=%s", rootIntent)
        mediaSession?.player?.run {
            if (playWhenReady || mediaItemCount == 0 || playbackState == Player.STATE_ENDED) {
                // stop player if it's not playing otherwise allow it to continue playing
                // in the background
                stopSelf()
            }
        }
    }

    override fun onPlaybackResumption(
        mediaSession: MediaSession,
        controller: MediaSession.ControllerInfo
    ): ListenableFuture<MediaItemsWithStartPosition> {
        Timber.d("onPlaybackResumption() mediaSession=%s, controller=%s", mediaSession, controller)
        return serviceScope.future(Dispatchers.Main) {
            Timber.d("onPlaybackResumption() future")
            MediaItemsWithStartPosition(
                player.playlist,
                player.currentPlaylistIndex,
                player.currentPosition
            )
        }
    }

    override fun onConnect(
        session: MediaSession,
        controller: MediaSession.ControllerInfo
    ): MediaSession.ConnectionResult {
        Timber.d("onConnect() session=%s, controller=%s", session, controller)
        val sessionCommands = MediaSession.ConnectionResult.DEFAULT_SESSION_AND_LIBRARY_COMMANDS
        if (
            session.isMediaNotificationController(controller) ||
            session.isAutomotiveController(controller) ||
            session.isAutoCompanionController(controller)
        ) {
            // Available session commands to accept incoming custom commands from Auto.
            return MediaSession.ConnectionResult.AcceptedResultBuilder(session)
                .setAvailableSessionCommands(sessionCommands)
                .build()
        }
        // Default commands with default custom layout for all other controllers.
        return MediaSession.ConnectionResult.AcceptedResultBuilder(session).build()
    }

    override fun onGetLibraryRoot(
        session: MediaLibrarySession,
        browser: MediaSession.ControllerInfo,
        params: LibraryParams?
    ): ListenableFuture<LibraryResult<MediaItem>> {
        Timber.d(
            "onGetLibraryRoot() session=%s, browser=%s, params=%s",
            session,
            browser,
            params
        )
        return serviceScope.future {
            val item = mediaItemTree.getRoot()
            Timber.d("onGetLibraryRoot() future result item=%s", MediaItemWrapper(item))
            LibraryResult.ofItem(item, params)
        }
    }

    override fun onGetChildren(
        session: MediaLibrarySession,
        browser: MediaSession.ControllerInfo,
        parentId: String,
        page: Int,
        pageSize: Int,
        params: LibraryParams?
    ): ListenableFuture<LibraryResult<ImmutableList<MediaItem>>> {
        Timber.d("onGetChildren() parentId=%s", parentId)
        return serviceScope.future {
            val items = mediaItemTree.getChildren(MediaId.fromString(parentId))
            Timber.d("onGetChildren() future result items=%s", MediaItemsWrapper(items))
            LibraryResult.ofItemList(items, params)
        }
    }

    override fun onGetItem(
        session: MediaLibrarySession,
        browser: MediaSession.ControllerInfo,
        mediaId: String
    ): ListenableFuture<LibraryResult<MediaItem>> {
        Timber.d("onGetItem() mediaId=%s", mediaId)
        return serviceScope.future {
            Timber.d("onPlaybackResumption() future")
            mediaItemTree.getItem(MediaId.fromString(mediaId)).toOption()
                .map { LibraryResult.ofItem(it, null) }
                .getOrElse { LibraryResult.ofError(SessionError.ERROR_INVALID_STATE) }
        }
    }

    override fun onAddMediaItems(
        mediaSession: MediaSession,
        controller: MediaSession.ControllerInfo,
        mediaItems: List<MediaItem>
    ): ListenableFuture<List<MediaItem>> {
        Timber.d("onAddMediaItems() mediaItems=%s", MediaItemsWrapper(mediaItems))
        return serviceScope.future {
            if (mediaItems.size == 1 && mediaItems.first().localConfiguration == null) {
                mediaItemTree.getChildren(MediaId.fromString(mediaItems.first().mediaId)).let { playlist ->
                    Timber.d("onAddMediaItems() future result playlist=%s", MediaItemsWrapper(playlist))
                    return@future playlist
                }
            }

            mediaItems.parMap {
                if (it.localConfiguration == null) {
                    mediaItemTree.getItem(it.realMediaId)
                } else {
                    it
                }
            }
        }
    }

    private fun setupCastPlayer() {
        runCatching {
            val castContext = CastContext.getSharedInstance(this, MoreExecutors.directExecutor())
                .addOnFailureListener {
                    Timber.e(it, "Error getting the cast session")
                }
                .result

            castPlayer = CastPlayer(castContext).apply {
                setSessionAvailabilityListener(
                    object : androidx.media3.cast.SessionAvailabilityListener {
                        override fun onCastSessionAvailable() {
                            castPlayer?.let {
                                player.setPlayer(it)
                            }
                        }

                        override fun onCastSessionUnavailable() {
                            exoPlayer?.let {
                                player.setPlayer(it)
                            }
                        }
                    }
                )
            }
        }.onFailure {
            Timber.e(it, "Error loading cast services")
        }
    }
}
