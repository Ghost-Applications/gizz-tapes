@file:Suppress("DEPRECATION")

package gizz.tapes.playback

import androidx.media3.common.AudioAttributes
import androidx.media3.common.DeviceInfo
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Metadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.common.Timeline
import androidx.media3.common.TrackSelectionParameters
import androidx.media3.common.Tracks
import androidx.media3.common.VideoSize
import androidx.media3.common.text.Cue
import androidx.media3.common.text.CueGroup
import androidx.media3.common.util.UnstableApi
import timber.log.Timber

@UnstableApi
class DelegatingPlayerListener(
    private val delegates: List<Player.Listener>
): Player.Listener {
    override fun onPlaybackStateChanged(playbackState: Int) {
        delegates.forEach { it.onPlaybackStateChanged(playbackState) }
    }

    override fun onTracksChanged(tracks: Tracks) {
        delegates.forEach { it.onTracksChanged(tracks) }
    }

    override fun onIsPlayingChanged(isPlaying: Boolean) {
        delegates.forEach { it.onIsPlayingChanged(isPlaying) }
    }

    override fun onPlayerError(error: PlaybackException) {
        Timber.e(error, "onPlayerError")
        delegates.forEach { it.onPlayerError(error) }
    }

    override fun onPlayerErrorChanged(error: PlaybackException?) {
        Timber.e(error, "onPlayerErrorChanged")
        delegates.forEach { it.onPlayerErrorChanged(error) }
    }

    override fun onTimelineChanged(timeline: Timeline, reason: Int) {
        delegates.forEach { it.onTimelineChanged(timeline, reason) }
    }

    override fun onPositionDiscontinuity(
        oldPosition: Player.PositionInfo,
        newPosition: Player.PositionInfo,
        reason: Int
    ) {
        delegates.forEach { it.onPositionDiscontinuity(oldPosition, newPosition, reason) }
    }

    override fun onAudioAttributesChanged(audioAttributes: AudioAttributes) {
        delegates.forEach { it.onAudioAttributesChanged(audioAttributes) }
    }

    override fun onAudioSessionIdChanged(audioSessionId: Int) {
        delegates.forEach { it.onAudioSessionIdChanged(audioSessionId) }
    }

    override fun onAvailableCommandsChanged(availableCommands: Player.Commands) {
        delegates.forEach { it.onAvailableCommandsChanged(availableCommands) }
    }

    override fun onCues(cueGroup: CueGroup) {
        delegates.forEach { it.onCues(cueGroup) }
    }

    override fun onDeviceInfoChanged(deviceInfo: DeviceInfo) {
        delegates.forEach { it.onDeviceInfoChanged(deviceInfo) }
    }

    override fun onIsLoadingChanged(isLoading: Boolean) {
        delegates.forEach { it.onIsLoadingChanged(isLoading) }
    }

    override fun onMaxSeekToPreviousPositionChanged(maxSeekToPreviousPositionMs: Long) {
        delegates.forEach { it.onMaxSeekToPreviousPositionChanged(maxSeekToPreviousPositionMs) }
    }

    override fun onMediaMetadataChanged(mediaMetadata: MediaMetadata) {
        delegates.forEach { it.onMediaMetadataChanged(mediaMetadata) }
    }

    override fun onPlaybackParametersChanged(playbackParameters: PlaybackParameters) {
        delegates.forEach { it.onPlaybackParametersChanged(playbackParameters) }
    }

    override fun onMetadata(metadata: Metadata) {
        delegates.forEach { it.onMetadata(metadata) }
    }

    override fun onPlaybackSuppressionReasonChanged(playbackSuppressionReason: Int) {
        delegates.forEach { it.onPlaybackSuppressionReasonChanged(playbackSuppressionReason) }
    }

    override fun onRenderedFirstFrame() {
        delegates.forEach { it.onRenderedFirstFrame() }
    }

    override fun onPlaylistMetadataChanged(mediaMetadata: MediaMetadata) {
        delegates.forEach { it.onPlaylistMetadataChanged(mediaMetadata) }
    }

    override fun onRepeatModeChanged(repeatMode: Int) {
        delegates.forEach { it.onRepeatModeChanged(repeatMode) }
    }

    override fun onSeekBackIncrementChanged(seekBackIncrementMs: Long) {
        delegates.forEach { it.onSeekBackIncrementChanged(seekBackIncrementMs) }
    }

    override fun onSeekForwardIncrementChanged(seekForwardIncrementMs: Long) {
        delegates.forEach { it.onSeekForwardIncrementChanged(seekForwardIncrementMs) }
    }

    override fun onShuffleModeEnabledChanged(shuffleModeEnabled: Boolean) {
        delegates.forEach { it.onShuffleModeEnabledChanged(shuffleModeEnabled) }
    }

    override fun onSkipSilenceEnabledChanged(skipSilenceEnabled: Boolean) {
        delegates.forEach { it.onSkipSilenceEnabledChanged(skipSilenceEnabled) }
    }

    override fun onTrackSelectionParametersChanged(parameters: TrackSelectionParameters) {
        delegates.forEach { it.onTrackSelectionParametersChanged(parameters) }
    }

    override fun onVideoSizeChanged(videoSize: VideoSize) {
        delegates.forEach { it.onVideoSizeChanged(videoSize) }
    }

    override fun onVolumeChanged(volume: Float) {
        delegates.forEach { it.onVolumeChanged(volume) }
    }

    override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
        delegates.forEach { it.onMediaItemTransition(mediaItem, reason) }
    }

    override fun onDeviceVolumeChanged(volume: Int, muted: Boolean) {
        delegates.forEach { it.onDeviceVolumeChanged(volume, muted) }
    }

    override fun onEvents(player: Player, events: Player.Events) {
        delegates.forEach { it.onEvents(player, events) }
    }

    override fun onPlayWhenReadyChanged(playWhenReady: Boolean, reason: Int) {
        delegates.forEach { it.onPlayWhenReadyChanged(playWhenReady, reason) }
    }

    override fun onSurfaceSizeChanged(width: Int, height: Int) {
        delegates.forEach { it.onSurfaceSizeChanged(width, height) }
    }

    @Deprecated("Deprecated in Java")
    override fun onCues(cues: MutableList<Cue>) {
        delegates.forEach { it.onCues(cues) }
    }

    @Deprecated("Deprecated in Java")
    override fun onLoadingChanged(isLoading: Boolean) {
        delegates.forEach { it.onLoadingChanged(isLoading) }
    }

    @Deprecated("Deprecated in Java")
    override fun onPositionDiscontinuity(reason: Int) {
        delegates.forEach { it.onPositionDiscontinuity(reason) }
    }

    @Deprecated("Deprecated in Java")
    override fun onPlayerStateChanged(playWhenReady: Boolean, playbackState: Int) {
        delegates.forEach { it.onPlayerStateChanged(playWhenReady, playbackState) }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as DelegatingPlayerListener

        return delegates == other.delegates
    }

    override fun hashCode(): Int {
        return delegates.hashCode()
    }

    override fun toString(): String {
        return "DelegatingPlayerListener(delegates=$delegates)"
    }
}