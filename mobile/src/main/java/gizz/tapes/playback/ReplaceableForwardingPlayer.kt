package gizz.tapes.playback

import android.os.Looper
import android.view.Surface
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.view.TextureView
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
import gizz.tapes.util.MediaItemWrapper
import gizz.tapes.util.MediaItemsWrapper
import timber.log.Timber
import kotlin.math.min

@UnstableApi
class ReplaceableForwardingPlayer(
    private var player: Player,
): Player {

    var playlist: MutableList<MediaItem> = mutableListOf()
        private set
    private val externalListeners: MutableList<Listener> = mutableListOf()
    var currentPlaylistIndex: Int = 0
        private set

    private val internalListener: Listener = DelegatingPlayerListener(externalListeners)

    init {
        player.addListener(internalListener)
    }

    fun setPlayer(newPlayer: Player) {
        player.removeListener(internalListener)
        newPlayer.addListener(internalListener)

        newPlayer.apply {
            playWhenReady = player.playWhenReady
            setMediaItems(playlist, currentPlaylistIndex, contentPosition)
            prepare()
        }

        player.clearMediaItems()
        player.stop()
        player = newPlayer
    }

    override fun getApplicationLooper(): Looper = player.applicationLooper

    override fun addListener(listener: Listener) {
        externalListeners.add(listener)
    }

    override fun removeListener(listener: Listener) {
        externalListeners.remove(listener)
    }

    override fun setMediaItems(mediaItems: List<MediaItem>) {
        player.setMediaItems(mediaItems)
        playlist.clear()
        playlist.addAll(mediaItems)
    }

    override fun setMediaItems(mediaItems: List<MediaItem>, resetPosition: Boolean) {
        Timber.d(
            "setMediaItems() mediaItems=%s resetPosition=%s",
            MediaItemsWrapper(mediaItems),
            resetPosition
        )
        player.setMediaItems(mediaItems, resetPosition)
        playlist.clear()
        playlist.addAll(mediaItems)
    }

    override fun setMediaItems(
        mediaItems: List<MediaItem>,
        startIndex: Int,
        startPositionMs: Long
    ) {
        currentPlaylistIndex = startIndex
        player.setMediaItems(mediaItems, startIndex, startPositionMs)
        playlist.clear()
        playlist.addAll(mediaItems)    }

    override fun setMediaItem(mediaItem: MediaItem) {
        player.setMediaItem(mediaItem)
        playlist.clear()
        playlist.add(mediaItem)
    }

    override fun setMediaItem(mediaItem: MediaItem, startPositionMs: Long) {
        player.setMediaItem(mediaItem, startPositionMs)
        playlist.clear()
        playlist.add(mediaItem)
    }

    override fun setMediaItem(mediaItem: MediaItem, resetPosition: Boolean) {
        player.setMediaItem(mediaItem, resetPosition)
        playlist.clear()
        playlist.add(mediaItem)
    }

    override fun addMediaItem(mediaItem: MediaItem) {
        player.addMediaItem(mediaItem)
        playlist.add(mediaItem)
    }

    override fun addMediaItem(index: Int, mediaItem: MediaItem) {
        player.addMediaItem(index, mediaItem)
        playlist.add(index, mediaItem)
    }

    override fun addMediaItems(mediaItems: List<MediaItem>) {
        player.addMediaItems(mediaItems)
        playlist.addAll(mediaItems)
    }

    override fun addMediaItems(index: Int, mediaItems: MutableList<MediaItem>) {
        player.addMediaItems(index, mediaItems)
        playlist.addAll(index, mediaItems)
    }

    override fun moveMediaItem(currentIndex: Int, newIndex: Int) {
        player.moveMediaItem(currentIndex, newIndex)
        playlist.add(min(newIndex, playlist.size), playlist.removeAt(currentIndex))
    }

    override fun moveMediaItems(fromIndex: Int, toIndex: Int, newIndex: Int) {
        val removedItems: ArrayDeque<MediaItem> = ArrayDeque()
        val removedItemsLength = toIndex - fromIndex
        for (i in removedItemsLength - 1 downTo 0) {
            removedItems.addFirst(playlist.removeAt(fromIndex + i))
        }
        playlist.addAll(min(newIndex, playlist.size), removedItems)
    }

    override fun replaceMediaItem(index: Int, mediaItem: MediaItem) {
        player.replaceMediaItem(index, mediaItem)
        playlist[index] = mediaItem
    }

    override fun replaceMediaItems(
        fromIndex: Int,
        toIndex: Int,
        mediaItems: MutableList<MediaItem>
    ) {
        player.replaceMediaItems(fromIndex, toIndex, mediaItems)
        mediaItems.forEachIndexed { index, mediaItem ->
            playlist[fromIndex + index] = mediaItem
        }
    }

    override fun removeMediaItem(index: Int) {
        player.removeMediaItem(index)
        playlist.removeAt(index)
    }

    override fun removeMediaItems(fromIndex: Int, toIndex: Int) {
        player.removeMediaItems(fromIndex, toIndex)
        val removedItemsLength = toIndex - fromIndex
        for (i in removedItemsLength - 1 downTo 0) {
            playlist.removeAt(fromIndex + i)
        }
    }

    override fun clearMediaItems() {
        player.clearMediaItems()
        playlist.clear()
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

    override fun play() = player.play()

    override fun pause() = player.pause()

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

    @Deprecated("Deprecated in Java")
    override fun hasPrevious(): Boolean = player.hasPrevious()

    @Deprecated("Deprecated in Java")
    override fun hasPreviousWindow(): Boolean = player.hasPreviousWindow()

    override fun hasPreviousMediaItem(): Boolean = player.hasPreviousMediaItem()

    @Deprecated("Deprecated in Java")
    override fun previous() = player.previous()

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

    override fun release() {
        player.release()
        playlist.clear()
    }

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