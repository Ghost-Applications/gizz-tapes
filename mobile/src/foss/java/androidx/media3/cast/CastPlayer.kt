package androidx.media3.cast

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
import androidx.media3.common.Timeline
import androidx.media3.common.TrackSelectionParameters
import androidx.media3.common.Tracks
import androidx.media3.common.VideoSize
import androidx.media3.common.text.CueGroup
import androidx.media3.common.util.Size
import androidx.media3.common.util.UnstableApi

@UnstableApi
@Suppress("ConvertSecondaryConstructorToPrimary", "OVERRIDE_DEPRECATION")
class CastPlayer : Player {
    constructor(input: Any) {
        error("Not implemented in FOSS variant")
    }

    fun setSessionAvailabilityListener(listener: SessionAvailabilityListener) {
        error("Not implemented in FOSS variant")
    }

    override fun getApplicationLooper(): Looper {
        error("Not implemented in FOSS variant")
    }

    override fun addListener(listener: Player.Listener) {
        error("Not implemented in FOSS variant")
    }

    override fun removeListener(listener: Player.Listener) {
        error("Not implemented in FOSS variant")
    }

    override fun setMediaItems(mediaItems: MutableList<MediaItem>) {
        error("Not implemented in FOSS variant")
    }

    override fun setMediaItems(mediaItems: MutableList<MediaItem>, resetPosition: Boolean) {
        error("Not implemented in FOSS variant")
    }

    override fun setMediaItems(
        mediaItems: MutableList<MediaItem>,
        startIndex: Int,
        startPositionMs: Long
    ) {
        error("Not implemented in FOSS variant")
    }

    override fun setMediaItem(mediaItem: MediaItem) {
        error("Not implemented in FOSS variant")
    }

    override fun setMediaItem(mediaItem: MediaItem, startPositionMs: Long) {
        error("Not implemented in FOSS variant")
    }

    override fun setMediaItem(mediaItem: MediaItem, resetPosition: Boolean) {
        error("Not implemented in FOSS variant")
    }

    override fun addMediaItem(mediaItem: MediaItem) {
        error("Not implemented in FOSS variant")
    }

    override fun addMediaItem(index: Int, mediaItem: MediaItem) {
        error("Not implemented in FOSS variant")
    }

    override fun addMediaItems(mediaItems: MutableList<MediaItem>) {
        error("Not implemented in FOSS variant")
    }

    override fun addMediaItems(index: Int, mediaItems: MutableList<MediaItem>) {
        error("Not implemented in FOSS variant")
    }

    override fun moveMediaItem(currentIndex: Int, newIndex: Int) {
        error("Not implemented in FOSS variant")
    }

    override fun moveMediaItems(fromIndex: Int, toIndex: Int, newIndex: Int) {
        error("Not implemented in FOSS variant")
    }

    override fun replaceMediaItem(index: Int, mediaItem: MediaItem) {
        error("Not implemented in FOSS variant")
    }

    override fun replaceMediaItems(
        fromIndex: Int,
        toIndex: Int,
        mediaItems: MutableList<MediaItem>
    ) {
        error("Not implemented in FOSS variant")
    }

    override fun removeMediaItem(index: Int) {
        error("Not implemented in FOSS variant")
    }

    override fun removeMediaItems(fromIndex: Int, toIndex: Int) {
        error("Not implemented in FOSS variant")
    }

    override fun clearMediaItems() {
        error("Not implemented in FOSS variant")
    }

    override fun isCommandAvailable(command: Int): Boolean {
        error("Not implemented in FOSS variant")
    }

    override fun canAdvertiseSession(): Boolean {
        error("Not implemented in FOSS variant")
    }

    override fun getAvailableCommands(): Player.Commands {
        error("Not implemented in FOSS variant")
    }

    override fun prepare() {
        error("Not implemented in FOSS variant")
    }

    override fun getPlaybackState(): Int {
        error("Not implemented in FOSS variant")
    }

    override fun getPlaybackSuppressionReason(): Int {
        error("Not implemented in FOSS variant")
    }

    override fun isPlaying(): Boolean {
        error("Not implemented in FOSS variant")
    }

    override fun getPlayerError(): PlaybackException? {
        error("Not implemented in FOSS variant")
    }

    override fun play() {
        error("Not implemented in FOSS variant")
    }

    override fun pause() {
        error("Not implemented in FOSS variant")
    }

    override fun setPlayWhenReady(playWhenReady: Boolean) {
        error("Not implemented in FOSS variant")
    }

    override fun getPlayWhenReady(): Boolean {
        error("Not implemented in FOSS variant")
    }

    override fun setRepeatMode(repeatMode: Int) {
        error("Not implemented in FOSS variant")
    }

    override fun getRepeatMode(): Int {
        error("Not implemented in FOSS variant")
    }

    override fun setShuffleModeEnabled(shuffleModeEnabled: Boolean) {
        error("Not implemented in FOSS variant")
    }

    override fun getShuffleModeEnabled(): Boolean {
        error("Not implemented in FOSS variant")
    }

    override fun isLoading(): Boolean {
        error("Not implemented in FOSS variant")
    }

    override fun seekToDefaultPosition() {
        error("Not implemented in FOSS variant")
    }

    override fun seekToDefaultPosition(mediaItemIndex: Int) {
        error("Not implemented in FOSS variant")
    }

    override fun seekTo(positionMs: Long) {
        error("Not implemented in FOSS variant")
    }

    override fun seekTo(mediaItemIndex: Int, positionMs: Long) {
        error("Not implemented in FOSS variant")
    }

    override fun getSeekBackIncrement(): Long {
        error("Not implemented in FOSS variant")
    }

    override fun seekBack() {
        error("Not implemented in FOSS variant")
    }

    override fun getSeekForwardIncrement(): Long {
        error("Not implemented in FOSS variant")
    }

    override fun seekForward() {
        error("Not implemented in FOSS variant")
    }

    override fun hasPreviousMediaItem(): Boolean {
        error("Not implemented in FOSS variant")
    }

    override fun seekToPreviousWindow() {
        error("Not implemented in FOSS variant")
    }

    override fun seekToPreviousMediaItem() {
        error("Not implemented in FOSS variant")
    }

    override fun getMaxSeekToPreviousPosition(): Long {
        error("Not implemented in FOSS variant")
    }

    override fun seekToPrevious() {
        error("Not implemented in FOSS variant")
    }

    override fun hasNext(): Boolean {
        error("Not implemented in FOSS variant")
    }

    override fun hasNextWindow(): Boolean {
        error("Not implemented in FOSS variant")
    }

    override fun hasNextMediaItem(): Boolean {
        error("Not implemented in FOSS variant")
    }

    override fun next() {
        error("Not implemented in FOSS variant")
    }

    override fun seekToNextWindow() {
        error("Not implemented in FOSS variant")
    }

    override fun seekToNextMediaItem() {
        error("Not implemented in FOSS variant")
    }

    override fun seekToNext() {
        error("Not implemented in FOSS variant")
    }

    override fun setPlaybackParameters(playbackParameters: PlaybackParameters) {
        error("Not implemented in FOSS variant")
    }

    override fun setPlaybackSpeed(speed: Float) {
        error("Not implemented in FOSS variant")
    }

    override fun getPlaybackParameters(): PlaybackParameters {
        error("Not implemented in FOSS variant")
    }

    override fun stop() {
        error("Not implemented in FOSS variant")
    }

    override fun release() {
        error("Not implemented in FOSS variant")
    }

    override fun getCurrentTracks(): Tracks {
        error("Not implemented in FOSS variant")
    }

    override fun getTrackSelectionParameters(): TrackSelectionParameters {
        error("Not implemented in FOSS variant")
    }

    override fun setTrackSelectionParameters(parameters: TrackSelectionParameters) {
        error("Not implemented in FOSS variant")
    }

    override fun getMediaMetadata(): MediaMetadata {
        error("Not implemented in FOSS variant")
    }

    override fun getPlaylistMetadata(): MediaMetadata {
        error("Not implemented in FOSS variant")
    }

    override fun setPlaylistMetadata(mediaMetadata: MediaMetadata) {
        error("Not implemented in FOSS variant")
    }

    override fun getCurrentManifest(): Any? {
        error("Not implemented in FOSS variant")
    }

    override fun getCurrentTimeline(): Timeline {
        error("Not implemented in FOSS variant")
    }

    override fun getCurrentPeriodIndex(): Int {
        error("Not implemented in FOSS variant")
    }

    override fun getCurrentWindowIndex(): Int {
        error("Not implemented in FOSS variant")
    }

    override fun getCurrentMediaItemIndex(): Int {
        error("Not implemented in FOSS variant")
    }

    override fun getNextWindowIndex(): Int {
        error("Not implemented in FOSS variant")
    }

    override fun getNextMediaItemIndex(): Int {
        error("Not implemented in FOSS variant")
    }

    override fun getPreviousWindowIndex(): Int {
        error("Not implemented in FOSS variant")
    }

    override fun getPreviousMediaItemIndex(): Int {
        error("Not implemented in FOSS variant")
    }

    override fun getCurrentMediaItem(): MediaItem? {
        error("Not implemented in FOSS variant")
    }

    override fun getMediaItemCount(): Int {
        error("Not implemented in FOSS variant")
    }

    override fun getMediaItemAt(index: Int): MediaItem {
        error("Not implemented in FOSS variant")
    }

    override fun getDuration(): Long {
        error("Not implemented in FOSS variant")
    }

    override fun getCurrentPosition(): Long {
        error("Not implemented in FOSS variant")
    }

    override fun getBufferedPosition(): Long {
        error("Not implemented in FOSS variant")
    }

    override fun getBufferedPercentage(): Int {
        error("Not implemented in FOSS variant")
    }

    override fun getTotalBufferedDuration(): Long {
        error("Not implemented in FOSS variant")
    }

    override fun isCurrentWindowDynamic(): Boolean {
        error("Not implemented in FOSS variant")
    }

    override fun isCurrentMediaItemDynamic(): Boolean {
        error("Not implemented in FOSS variant")
    }

    override fun isCurrentWindowLive(): Boolean {
        error("Not implemented in FOSS variant")
    }

    override fun isCurrentMediaItemLive(): Boolean {
        error("Not implemented in FOSS variant")
    }

    override fun getCurrentLiveOffset(): Long {
        error("Not implemented in FOSS variant")
    }

    override fun isCurrentWindowSeekable(): Boolean {
        error("Not implemented in FOSS variant")
    }

    override fun isCurrentMediaItemSeekable(): Boolean {
        error("Not implemented in FOSS variant")
    }

    override fun isPlayingAd(): Boolean {
        error("Not implemented in FOSS variant")
    }

    override fun getCurrentAdGroupIndex(): Int {
        error("Not implemented in FOSS variant")
    }

    override fun getCurrentAdIndexInAdGroup(): Int {
        error("Not implemented in FOSS variant")
    }

    override fun getContentDuration(): Long {
        error("Not implemented in FOSS variant")
    }

    override fun getContentPosition(): Long {
        error("Not implemented in FOSS variant")
    }

    override fun getContentBufferedPosition(): Long {
        error("Not implemented in FOSS variant")
    }

    override fun getAudioAttributes(): AudioAttributes {
        error("Not implemented in FOSS variant")
    }

    override fun setVolume(volume: Float) {
        error("Not implemented in FOSS variant")
    }

    override fun getVolume(): Float {
        error("Not implemented in FOSS variant")
    }

    override fun clearVideoSurface() {
        error("Not implemented in FOSS variant")
    }

    override fun clearVideoSurface(surface: Surface?) {
        error("Not implemented in FOSS variant")
    }

    override fun setVideoSurface(surface: Surface?) {
        error("Not implemented in FOSS variant")
    }

    override fun setVideoSurfaceHolder(surfaceHolder: SurfaceHolder?) {
        error("Not implemented in FOSS variant")
    }

    override fun clearVideoSurfaceHolder(surfaceHolder: SurfaceHolder?) {
        error("Not implemented in FOSS variant")
    }

    override fun setVideoSurfaceView(surfaceView: SurfaceView?) {
        error("Not implemented in FOSS variant")
    }

    override fun clearVideoSurfaceView(surfaceView: SurfaceView?) {
        error("Not implemented in FOSS variant")
    }

    override fun setVideoTextureView(textureView: TextureView?) {
        error("Not implemented in FOSS variant")
    }

    override fun clearVideoTextureView(textureView: TextureView?) {
        error("Not implemented in FOSS variant")
    }

    override fun getVideoSize(): VideoSize {
        error("Not implemented in FOSS variant")
    }

    override fun getSurfaceSize(): Size {
        error("Not implemented in FOSS variant")
    }

    override fun getCurrentCues(): CueGroup {
        error("Not implemented in FOSS variant")
    }

    override fun getDeviceInfo(): DeviceInfo {
        error("Not implemented in FOSS variant")
    }

    override fun getDeviceVolume(): Int {
        error("Not implemented in FOSS variant")
    }

    override fun isDeviceMuted(): Boolean {
        error("Not implemented in FOSS variant")
    }

    override fun setDeviceVolume(volume: Int) {
        error("Not implemented in FOSS variant")
    }

    override fun setDeviceVolume(volume: Int, flags: Int) {
        error("Not implemented in FOSS variant")
    }

    override fun increaseDeviceVolume() {
        error("Not implemented in FOSS variant")
    }

    override fun increaseDeviceVolume(flags: Int) {
        error("Not implemented in FOSS variant")
    }

    override fun decreaseDeviceVolume() {
        error("Not implemented in FOSS variant")
    }

    override fun decreaseDeviceVolume(flags: Int) {
        error("Not implemented in FOSS variant")
    }

    override fun setDeviceMuted(muted: Boolean) {
        error("Not implemented in FOSS variant")
    }

    override fun setDeviceMuted(muted: Boolean, flags: Int) {
        error("Not implemented in FOSS variant")
    }

    override fun setAudioAttributes(audioAttributes: AudioAttributes, handleAudioFocus: Boolean) {
        error("Not implemented in FOSS variant")
    }
}
