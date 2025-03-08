package gizz.tapes.playback

import android.os.Looper
import android.view.Surface
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.view.TextureView
import androidx.annotation.OptIn
import androidx.media3.common.AudioAttributes
import androidx.media3.common.DeviceInfo
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.common.Player.Listener
import androidx.media3.common.Timeline
import androidx.media3.common.TrackSelectionParameters
import androidx.media3.common.Tracks
import androidx.media3.common.VideoSize
import androidx.media3.common.text.CueGroup
import androidx.media3.common.util.Size
import androidx.media3.common.util.UnstableApi
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import gizz.tapes.util.MediaItemWrapper
import gizz.tapes.util.MediaItemsWrapper
import gizz.tapes.util.MediaMetaDataWrapper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.cancel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import kotlin.math.min
import kotlin.time.Duration.Companion.seconds

@AssistedFactory
interface ReplaceableForwardingPlayerFactory {
    @OptIn(UnstableApi::class)
    fun create(player: Player): ReplaceableForwardingPlayer
}

@UnstableApi
class ReplaceableForwardingPlayer @AssistedInject constructor(
    private val currentlyPlayingSaver: CurrentlyPlayingSaver,
    @Assisted private var player: Player,
): Player {

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private val _playlist: MutableList<MediaItem> = mutableListOf()
    private val externalListeners: MutableList<Listener> = mutableListOf()

    var currentPlaylistIndex: Int = 0
        private set

    val playlist: List<MediaItem> = _playlist

    private val internalListener: Listener = DelegatingPlayerListener(externalListeners)

    private val replaceableForwardingPlayerListener = object : Listener {
        var job: Job? = null

        override fun onPositionDiscontinuity(
            oldPosition: Player.PositionInfo,
            newPosition: Player.PositionInfo,
            reason: Int
        ) = updateCurrentPlaylistIndex()

        override fun onMediaItemTransition(
            mediaItem: MediaItem?, reason: Int
        ) = updateCurrentPlaylistIndex()

        override fun onTimelineChanged(
            timeline: Timeline, reason: Int
        ) = updateCurrentPlaylistIndex()

        override fun onIsPlayingChanged(isPlaying: Boolean) {
            Timber.d("onIsPlayingChanged() isPlaying=%s", isPlaying)
            when (isPlaying) {
                true -> {
                    job = scope.launch {
                        while(coroutineContext.isActive) {
                            delay(5.seconds)
                            saveState()
                        }
                    }
                }
                false -> job?.cancel()
            }
        }

        fun updateCurrentPlaylistIndex() {
            Timber.d("updateCurrentPlaylistIndex()")
            if (!player.currentTimeline.isEmpty) {
                currentPlaylistIndex = player.currentMediaItemIndex
            }
        }
    }

    init {
        Timber.d("init() ->")
        externalListeners.add(replaceableForwardingPlayerListener)
        player.addListener(internalListener)

        scope.launch {
            val mediaItems = async { currentlyPlayingSaver.mediaItems() }
            val currentTrack = async { currentlyPlayingSaver.currentTrack() }
            val currentPosition = async { currentlyPlayingSaver.currentPosition() }

            withContext(Dispatchers.Main) {
                setMediaItems(mediaItems.await(), true)
                seekTo(currentTrack.await(), currentPosition.await())
                Timber.d("init() withContext complete")
            }
        }
        Timber.d("init() <-")
    }

    private suspend fun saveState() {
        coroutineScope {
            val currentPosition = async(Dispatchers.Main) { currentPosition }

            currentlyPlayingSaver.saveCurrentState(
                mediaItems = playlist,
                currentTrackIndex = currentPlaylistIndex,
                currentPosition = currentPosition.await()
            )
        }
    }

    fun setPlayer(newPlayer: Player) {
        Timber.d("setPlayer() newPlayer=%s", newPlayer)
        player.removeListener(internalListener)
        newPlayer.addListener(internalListener)

        newPlayer.apply {
            playWhenReady = player.playWhenReady
            setMediaItems(_playlist, currentPlaylistIndex, contentPosition)
            prepare()
        }

        player.clearMediaItems()
        player.stop()
        player = newPlayer
    }

    override fun release() {
        Timber.d("release()")
        scope.cancel()
        player.release()
    }

    override fun getApplicationLooper(): Looper = player.applicationLooper

    override fun addListener(listener: Listener) {
        Timber.d("addListener()")
        externalListeners.add(listener)
    }

    override fun removeListener(listener: Listener) {
        Timber.d("removeListener()")
        externalListeners.remove(listener)
    }

    override fun setMediaItems(mediaItems: List<MediaItem>) {
        Timber.d("setMediaItems mediaItems=%s", MediaItemsWrapper(mediaItems))
        player.setMediaItems(mediaItems)
        _playlist.clear()
        _playlist.addAll(mediaItems)
    }

    override fun setMediaItems(mediaItems: List<MediaItem>, resetPosition: Boolean) {
        Timber.d(
            "setMediaItems() mediaItems=%s resetPosition=%s",
            MediaItemsWrapper(mediaItems),
            resetPosition
        )
        player.setMediaItems(mediaItems, resetPosition)
        _playlist.clear()
        _playlist.addAll(mediaItems)
    }

    override fun setMediaItems(
        mediaItems: List<MediaItem>,
        startIndex: Int,
        startPositionMs: Long
    ) {
        Timber.d("setMediaItems() mediaItems=%s startIndex=%s startPositionMs=%s",
            MediaItemsWrapper(mediaItems),
            startIndex,
            startPositionMs
        )
        currentPlaylistIndex = startIndex
        player.setMediaItems(mediaItems, startIndex, startPositionMs)
        _playlist.clear()
        _playlist.addAll(mediaItems)    }

    override fun setMediaItem(mediaItem: MediaItem) {
        Timber.d("setMediaItem() mediaItem=%s", MediaItemWrapper(mediaItem))
        player.setMediaItem(mediaItem)
        _playlist.clear()
        _playlist.add(mediaItem)
    }

    override fun setMediaItem(mediaItem: MediaItem, startPositionMs: Long) {
        Timber.d(
            "setMediaItem() mediaItem=%s startPositionMs=%s",
            MediaItemWrapper(mediaItem),
            startPositionMs
        )
        player.setMediaItem(mediaItem, startPositionMs)
        _playlist.clear()
        _playlist.add(mediaItem)
    }

    override fun setMediaItem(mediaItem: MediaItem, resetPosition: Boolean) {
        Timber.d(
            "setMediaItem() mediaItem=%s resetPosition=%s",
            MediaItemWrapper(mediaItem),
            resetPosition
        )
        player.setMediaItem(mediaItem, resetPosition)
        _playlist.clear()
        _playlist.add(mediaItem)
    }

    override fun addMediaItem(mediaItem: MediaItem) {
        Timber.d("addMediaItem() mediaItem=%s", MediaItemWrapper(mediaItem))
        player.addMediaItem(mediaItem)
        _playlist.add(mediaItem)
    }

    override fun addMediaItem(index: Int, mediaItem: MediaItem) {
        Timber.d("addMediaItem() index=%s mediaItem=%s", index, MediaItemWrapper(mediaItem))
        player.addMediaItem(index, mediaItem)
        _playlist.add(index, mediaItem)
    }

    override fun addMediaItems(mediaItems: List<MediaItem>) {
        Timber.d("addMediaItems() mediaItems=%s", MediaItemsWrapper(mediaItems))
        player.addMediaItems(mediaItems)
        _playlist.addAll(mediaItems)
    }

    override fun addMediaItems(index: Int, mediaItems: MutableList<MediaItem>) {
        Timber.d("addMediaItems() index=%s mediaItems=%s", index, MediaItemsWrapper(mediaItems))
        player.addMediaItems(index, mediaItems)
        _playlist.addAll(index, mediaItems)
    }

    override fun moveMediaItem(currentIndex: Int, newIndex: Int) {
        Timber.d("moveMediaItem() currentIndex=%s newIndex=%s", currentIndex, newIndex)
        player.moveMediaItem(currentIndex, newIndex)
        _playlist.add(min(newIndex, _playlist.size), _playlist.removeAt(currentIndex))
    }

    override fun moveMediaItems(fromIndex: Int, toIndex: Int, newIndex: Int) {
        Timber.d("moveMediaItems() fromIndex=%s toIndex=%s newIndex=%s", fromIndex, toIndex, newIndex)
        val removedItems: ArrayDeque<MediaItem> = ArrayDeque()
        val removedItemsLength = toIndex - fromIndex
        for (i in removedItemsLength - 1 downTo 0) {
            removedItems.addFirst(_playlist.removeAt(fromIndex + i))
        }
        _playlist.addAll(min(newIndex, _playlist.size), removedItems)
    }

    override fun replaceMediaItem(index: Int, mediaItem: MediaItem) {
        Timber.d("replaceMediaItem() index=%s mediaItem=%s", index, MediaItemWrapper(mediaItem))
        player.replaceMediaItem(index, mediaItem)
        _playlist[index] = mediaItem
    }

    override fun replaceMediaItems(
        fromIndex: Int,
        toIndex: Int,
        mediaItems: MutableList<MediaItem>
    ) {
        Timber.d(
            "replaceMediaItems() fromIndex=%s toIndex=%s mediaItems=%s",
            fromIndex,
            toIndex,
            MediaItemsWrapper(mediaItems)
        )
        player.replaceMediaItems(fromIndex, toIndex, mediaItems)
        mediaItems.forEachIndexed { index, mediaItem ->
            _playlist[fromIndex + index] = mediaItem
        }
    }

    override fun removeMediaItem(index: Int) {
        Timber.d("removeMediaItem() index=%s", index)
        player.removeMediaItem(index)
        _playlist.removeAt(index)
    }

    override fun removeMediaItems(fromIndex: Int, toIndex: Int) {
        player.removeMediaItems(fromIndex, toIndex)
        val removedItemsLength = toIndex - fromIndex
        for (i in removedItemsLength - 1 downTo 0) {
            _playlist.removeAt(fromIndex + i)
        }
    }

    override fun clearMediaItems() {
        player.clearMediaItems()
        _playlist.clear()
        currentPlaylistIndex = 0
    }

    override fun isCommandAvailable(command: Int): Boolean = player.isCommandAvailable(command)

    override fun canAdvertiseSession(): Boolean = player.canAdvertiseSession()

    override fun getAvailableCommands(): Player.Commands = player.availableCommands

    override fun prepare() = player.prepare()

    override fun getPlaybackState(): Int = player.playbackState

    override fun getPlaybackSuppressionReason(): Int = player.playbackSuppressionReason

    override fun isPlaying(): Boolean = player.isPlaying

    override fun getPlayerError(): PlaybackException? = player.playerError

    override fun play() {
        Timber.d("play()")
        player.play()
    }

    override fun pause() {
        Timber.d("pause()")
        player.pause()
    }

    override fun setPlayWhenReady(playWhenReady: Boolean) {
        player.playWhenReady = playWhenReady
    }

    override fun getPlayWhenReady(): Boolean = player.playWhenReady

    override fun setRepeatMode(repeatMode: Int) {
        player.repeatMode = repeatMode
    }

    override fun getRepeatMode(): Int = player.repeatMode

    override fun setShuffleModeEnabled(shuffleModeEnabled: Boolean) {
        player.shuffleModeEnabled = shuffleModeEnabled
    }

    override fun getShuffleModeEnabled(): Boolean = player.shuffleModeEnabled

    override fun isLoading(): Boolean = player.isLoading

    override fun seekToDefaultPosition() = player.seekToDefaultPosition()

    override fun seekToDefaultPosition(windowIndex: Int) = player.seekToDefaultPosition(windowIndex)

    override fun seekTo(positionMs: Long) = player.seekTo(positionMs)

    override fun seekTo(mediaItemIndex: Int, positionMs: Long) = player.seekTo(mediaItemIndex, positionMs)

    override fun getSeekBackIncrement(): Long = player.seekBackIncrement

    override fun seekBack() = player.seekBack()

    override fun getSeekForwardIncrement(): Long = player.seekForwardIncrement

    override fun seekForward() = player.seekForward()

    override fun hasPreviousMediaItem(): Boolean = player.hasPreviousMediaItem()

    @Deprecated("Deprecated in Java")
    override fun seekToPreviousWindow() = player.seekToPreviousWindow()

    override fun seekToPreviousMediaItem() = player.seekToPreviousMediaItem()

    override fun getMaxSeekToPreviousPosition(): Long = player.maxSeekToPreviousPosition

    override fun seekToPrevious() = player.seekToPrevious()

    @Deprecated("Deprecated in Java")
    override fun hasNext(): Boolean = player.hasNext()

    @Deprecated("Deprecated in Java")
    override fun hasNextWindow(): Boolean = player.hasNextWindow()

    override fun hasNextMediaItem(): Boolean = player.hasNextMediaItem()

    @Deprecated("Deprecated in Java")
    override fun next() = player.next()

    @Deprecated("Deprecated in Java")
    override fun seekToNextWindow() = player.seekToNextWindow()

    override fun seekToNextMediaItem() = player.seekToNextMediaItem()

    override fun seekToNext() = player.seekToNext()

    override fun setPlaybackParameters(playbackParameters: PlaybackParameters) {
        player.playbackParameters = playbackParameters
    }

    override fun setPlaybackSpeed(speed: Float) = player.setPlaybackSpeed(speed)

    override fun getPlaybackParameters(): PlaybackParameters = player.playbackParameters

    override fun stop() = player.stop()

    override fun getCurrentTracks(): Tracks = player.currentTracks

    override fun getTrackSelectionParameters(): TrackSelectionParameters {
        return player.trackSelectionParameters
    }

    override fun setTrackSelectionParameters(parameters: TrackSelectionParameters) {
        player.trackSelectionParameters = parameters
    }

    override fun getMediaMetadata(): MediaMetadata = player.mediaMetadata

    override fun getPlaylistMetadata(): MediaMetadata = player.playlistMetadata

    override fun setPlaylistMetadata(mediaMetadata: MediaMetadata) {
        Timber.d("setPlaylistMetadata() mediaMetadata=%s", MediaMetaDataWrapper(mediaMetadata))
        player.playlistMetadata = mediaMetadata
    }

    override fun getCurrentManifest(): Any? = player.currentManifest

    override fun getCurrentTimeline(): Timeline = player.currentTimeline

    override fun getCurrentPeriodIndex(): Int = player.currentPeriodIndex

    @Deprecated("Deprecated in Java")
    override fun getCurrentWindowIndex(): Int = player.currentWindowIndex

    override fun getCurrentMediaItemIndex(): Int = player.currentMediaItemIndex

    @Deprecated("Deprecated in Java")
    override fun getNextWindowIndex(): Int = player.nextWindowIndex

    override fun getNextMediaItemIndex(): Int {
        return player.nextMediaItemIndex
    }

    @Deprecated("Deprecated in Java")
    override fun getPreviousWindowIndex(): Int = player.previousWindowIndex

    override fun getPreviousMediaItemIndex(): Int = player.previousMediaItemIndex

    override fun getCurrentMediaItem(): MediaItem? = player.currentMediaItem

    override fun getMediaItemCount(): Int = player.mediaItemCount

    override fun getMediaItemAt(index: Int): MediaItem = player.getMediaItemAt(index)

    override fun getDuration(): Long = player.duration

    override fun getCurrentPosition(): Long = player.currentPosition

    override fun getBufferedPosition(): Long = player.bufferedPosition

    override fun getBufferedPercentage(): Int = player.bufferedPercentage

    override fun getTotalBufferedDuration(): Long = player.totalBufferedDuration

    @Deprecated("Deprecated in Java")
    override fun isCurrentWindowDynamic(): Boolean = player.isCurrentWindowDynamic

    override fun isCurrentMediaItemDynamic(): Boolean = player.isCurrentMediaItemDynamic

    @Deprecated("Deprecated in Java")
    override fun isCurrentWindowLive(): Boolean = player.isCurrentWindowLive

    override fun isCurrentMediaItemLive(): Boolean = player.isCurrentMediaItemLive

    override fun getCurrentLiveOffset(): Long = player.currentLiveOffset

    @Deprecated("Deprecated in Java")
    override fun isCurrentWindowSeekable(): Boolean = player.isCurrentWindowSeekable

    override fun isCurrentMediaItemSeekable(): Boolean = player.isCurrentMediaItemSeekable

    override fun isPlayingAd(): Boolean = player.isPlayingAd

    override fun getCurrentAdGroupIndex(): Int = player.currentAdGroupIndex

    override fun getCurrentAdIndexInAdGroup(): Int = player.currentAdIndexInAdGroup

    override fun getContentDuration(): Long = player.contentDuration

    override fun getContentPosition(): Long = player.contentPosition

    override fun getContentBufferedPosition(): Long = player.contentBufferedPosition

    override fun getAudioAttributes(): AudioAttributes = player.audioAttributes

    override fun setVolume(volume: Float) {
        player.volume = volume
    }

    override fun getVolume(): Float = player.volume

    override fun clearVideoSurface() = player.clearVideoSurface()

    override fun clearVideoSurface(surface: Surface?) = player.clearVideoSurface(surface)

    override fun setVideoSurface(surface: Surface?) = player.setVideoSurface(surface)

    override fun setVideoSurfaceHolder(surfaceHolder: SurfaceHolder?) {
        player.setVideoSurfaceHolder(surfaceHolder)
    }

    override fun clearVideoSurfaceHolder(surfaceHolder: SurfaceHolder?) {
        player.clearVideoSurfaceHolder(surfaceHolder)
    }

    override fun setVideoSurfaceView(surfaceView: SurfaceView?) {
        return player.setVideoSurfaceView(surfaceView)
    }

    override fun clearVideoSurfaceView(surfaceView: SurfaceView?) {
        return player.clearVideoSurfaceView(surfaceView)
    }

    override fun setVideoTextureView(textureView: TextureView?) {
        return player.setVideoTextureView(textureView)
    }

    override fun clearVideoTextureView(textureView: TextureView?) {
        return player.clearVideoTextureView(textureView)
    }

    override fun getVideoSize(): VideoSize = player.videoSize
    override fun getSurfaceSize(): Size = player.surfaceSize
    override fun getCurrentCues(): CueGroup = player.currentCues
    override fun getDeviceInfo(): DeviceInfo = player.deviceInfo
    override fun getDeviceVolume(): Int = player.deviceVolume
    override fun isDeviceMuted(): Boolean = player.isDeviceMuted

    @Deprecated("Deprecated in Java")
    override fun setDeviceVolume(volume: Int) {
        player.deviceVolume = volume
    }

    override fun setDeviceVolume(volume: Int, flags: Int) {
        player.setDeviceVolume(volume, flags)
    }

    @Deprecated("Deprecated in Java")
    override fun increaseDeviceVolume() = player.increaseDeviceVolume()

    override fun increaseDeviceVolume(flags: Int) {
        player.increaseDeviceVolume(flags)
    }

    @Deprecated("Deprecated in Java")
    override fun decreaseDeviceVolume() = player.decreaseDeviceVolume()

    override fun decreaseDeviceVolume(flags: Int) {
        player.decreaseDeviceVolume(flags)
    }

    @Deprecated("Deprecated in Java")
    override fun setDeviceMuted(muted: Boolean) {
        player.isDeviceMuted = muted
    }

    override fun setDeviceMuted(muted: Boolean, flags: Int) {
        player.setDeviceMuted(muted, flags)
    }

    override fun setAudioAttributes(audioAttributes: AudioAttributes, handleAudioFocus: Boolean) {
        player.setAudioAttributes(audioAttributes, handleAudioFocus)
    }
}
