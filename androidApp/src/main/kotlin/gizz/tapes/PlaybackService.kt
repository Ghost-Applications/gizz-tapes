package gizz.tapes

import android.app.PendingIntent
import android.app.PendingIntent.FLAG_IMMUTABLE
import android.app.PendingIntent.FLAG_UPDATE_CURRENT
import android.app.Service
import android.content.Intent
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.DeviceInfo
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.audio.MediaCodecAudioRenderer
import androidx.media3.exoplayer.mediacodec.MediaCodecSelector
import androidx.media3.session.LibraryResult
import androidx.media3.session.MediaLibraryService
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSession.MediaItemsWithStartPosition
import androidx.media3.session.SessionError
import arrow.core.getOrElse
import arrow.core.toOption
import arrow.fx.coroutines.parMap
import co.touchlab.kermit.Logger
import com.google.common.collect.ImmutableList
import com.google.common.util.concurrent.ListenableFuture
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesIntoMap
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.binding
import dev.zacsweers.metrox.android.ServiceKey
import gizz.tapes.playback.CurrentlyPlayingSaver
import gizz.tapes.playback.MediaId
import gizz.tapes.playback.MediaItemTree
import gizz.tapes.playback.PlayerFactory
import gizz.tapes.util.MediaItemWrapper
import gizz.tapes.util.MediaItemsWrapper
import gizz.tapes.util.realMediaId
import gizz.tapes.util.toMediaItems
import gizz.tapes.util.toPlaybackItems
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.guava.future
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.time.Duration.Companion.seconds

@Inject
@UnstableApi
@ServiceKey
@ContributesIntoMap(AppScope::class, binding<Service>())
class PlaybackService(
    private val playerFactory: PlayerFactory,
    private val currentlyPlayingSaver: CurrentlyPlayingSaver,
    private val mediaItemTree: MediaItemTree
) : MediaLibraryService(), MediaLibraryService.MediaLibrarySession.Callback {

    private val logger = Logger.withTag("PlaybackService")

    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private var mediaSession: MediaLibrarySession? = null
    private lateinit var player: Player
    private lateinit var initJob: Job

    override fun onCreate() {
        logger.d { "onCreate()" }
        super.onCreate()

        val exoPlayer = ExoPlayer.Builder(this)
            // only load renderers for audio files since we do not support video
            // this should decrease the release app size.
            .setRenderersFactory { eventHandler, _, audioRendererEventListener, _, _ ->
                arrayOf(
                    MediaCodecAudioRenderer(
                        applicationContext,
                        MediaCodecSelector.DEFAULT,
                        eventHandler,
                        audioRendererEventListener
                    )
                )
            }
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

        player = playerFactory.create(exoPlayer)

        initJob = serviceScope.launch {
            val storedSession = currentlyPlayingSaver.storedSession()
            withContext(Dispatchers.Main) {
                player.setMediaItems(storedSession.items.toMediaItems(), true)
                player.seekTo(storedSession.currentTrack, storedSession.currentTime)
            }
        }

        player.addListener(object : Player.Listener {
            private var pollingJob: Job? = null

            override fun onIsPlayingChanged(isPlaying: Boolean) {
                logger.d { "onIsPlayingChanged() isPlaying=$isPlaying" }
                if (isPlaying) {
                    pollingJob = serviceScope.launch {
                        while (coroutineContext.isActive) {
                            delay(5.seconds)
                            saveState()
                        }
                    }
                } else {
                    pollingJob?.cancel()
                    serviceScope.launch { saveState() }
                }
            }
        })

        val pendingIntent = PendingIntent.getActivity(
            this,
            1337,
            Intent(this, MainActivity::class.java),
            FLAG_IMMUTABLE or FLAG_UPDATE_CURRENT
        )

        mediaSession = MediaLibrarySession.Builder(this, player, this)
            .setSessionActivity(pendingIntent)
            .build()
    }

    override fun onDestroy() {
        logger.d { "onDestroy()" }
        mediaSession?.run {
            player.release()
            release()
            mediaSession = null
        }
        serviceScope.cancel()
        super.onDestroy()
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaLibrarySession? {
        Logger.d { "onGetSession controllerInfo=$controllerInfo" }
        return mediaSession
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        logger.d { "onTaskRemoved() rootIntent=$rootIntent" }
        mediaSession?.player?.run {
            val isCasting = deviceInfo.playbackType == DeviceInfo.PLAYBACK_TYPE_REMOTE
            val shouldStop = playWhenReady || mediaItemCount == 0 || playbackState == Player.STATE_ENDED
            if (!isCasting) {
                serviceScope.launch {
                    saveState()
                    if (shouldStop) stopSelf()
                }
            }
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onPlaybackResumption(
        mediaSession: MediaSession,
        controller: MediaSession.ControllerInfo
    ): ListenableFuture<MediaItemsWithStartPosition> {
        Logger.d { "onPlaybackResumption() mediaSession=$mediaSession, controller=$controller" }
        return serviceScope.future(Dispatchers.Main) {
            Logger.d("onPlaybackResumption() future")
            initJob.join()
            MediaItemsWithStartPosition(
                (0 until player.mediaItemCount).map { player.getMediaItemAt(it) },
                player.currentMediaItemIndex,
                player.currentPosition
            )
        }
    }

    override fun onConnect(
        session: MediaSession,
        controller: MediaSession.ControllerInfo
    ): MediaSession.ConnectionResult {
        Logger.d { "onConnect() session=$session, controller=$controller" }
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
        Logger.d {
            "onGetLibraryRoot() session=$session, browser=$browser, params=$params"
        }
        return serviceScope.future {
            val item = mediaItemTree.getRoot()
            Logger.d { "onGetLibraryRoot() future result item=${MediaItemWrapper(item)}" }
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
        Logger.d { "onGetChildren() parentId=$parentId" }
        return serviceScope.future {
            val items = mediaItemTree.getChildren(MediaId.fromString(parentId))
            Logger.d { "onGetChildren() future result items=${MediaItemsWrapper(items)}" }
            LibraryResult.ofItemList(items, params)
        }
    }

    override fun onGetItem(
        session: MediaLibrarySession,
        browser: MediaSession.ControllerInfo,
        mediaId: String
    ): ListenableFuture<LibraryResult<MediaItem>> {
        Logger.d { "onGetItem() mediaId=$mediaId" }
        return serviceScope.future {
            Logger.d { "onGetItem() future" }
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
        Logger.d { "onAddMediaItems() mediaItems=${MediaItemsWrapper(mediaItems)}" }
        return serviceScope.future {
            if (mediaItems.size == 1 && mediaItems.first().localConfiguration == null) {
                mediaItemTree.getChildren(MediaId.fromString(mediaItems.first().mediaId)).let { playlist ->
                    Logger.d { "onAddMediaItems() future result playlist=${MediaItemsWrapper(playlist)}" }
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

    private suspend fun saveState() {
        val (items, trackIndex, position) = withContext(Dispatchers.Main) {
            Triple(
                (0 until player.mediaItemCount).map { player.getMediaItemAt(it) },
                player.currentMediaItemIndex,
                player.currentPosition
            )
        }
        currentlyPlayingSaver.save(
            items = items.toPlaybackItems(),
            currentTrackIndex = trackIndex,
            currentPosition = position
        )
    }
}
