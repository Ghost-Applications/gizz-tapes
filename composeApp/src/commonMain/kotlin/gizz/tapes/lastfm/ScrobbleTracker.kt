package gizz.tapes.lastfm

import gizz.tapes.ui.player.PlayerState
import kotlin.time.Clock
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant

/**
 * Turns [PlayerState] updates into Last.fm events, following https://www.last.fm/api/scrobbling:
 * a track is scrobbled once it is longer than 30s and has played for half its length or 4 minutes,
 * whichever comes first. Only actual listening time counts, so seeking ahead doesn't.
 */
class ScrobbleTracker(private val clock: Clock = Clock.System) {

    sealed interface Event {
        data class NowPlaying(val track: LastFmTrack) : Event
        data class Scrobble(val scrobble: LastFmScrobble) : Event
    }

    private class Tracked(val mediaId: String) {
        var startedAt: Instant? = null
        var playedMs = 0L
        var lastPositionMs = 0L
        var lastTickAt: Instant? = null
        var scrobbled = false
    }

    private var current: Tracked? = null

    fun onState(state: PlayerState): Event? {
        if (state !is PlayerState.MediaLoaded) {
            current = null
            return null
        }

        val tracked = current?.takeIf { it.mediaId == state.mediaId } ?: Tracked(state.mediaId).also { current = it }
        val now = clock.now()
        val position = state.durationInfo.currentPosition
        val track = LastFmTrack(
            artist = state.artistName,
            track = state.title,
            album = state.albumTitle,
            durationSeconds = state.durationInfo.duration.milliseconds.inWholeSeconds.takeIf { it > 0 },
        )

        var event: Event? = null
        if (state is PlayerState.MediaLoaded.Playing) {
            val lastTickAt = tracked.lastTickAt
            if (lastTickAt == null) {
                if (tracked.startedAt == null) {
                    tracked.startedAt = now
                    event = Event.NowPlaying(track)
                }
            } else {
                // Position moving further than wall-clock time allows means a seek, not listening.
                val delta = position - tracked.lastPositionMs
                if (delta > 0 && delta <= (now - lastTickAt).inWholeMilliseconds + TICK_SLACK_MS) {
                    tracked.playedMs += delta
                }
            }
            tracked.lastTickAt = now
        } else {
            tracked.lastTickAt = null
        }
        tracked.lastPositionMs = position

        val startedAt = tracked.startedAt
        if (!tracked.scrobbled && startedAt != null && meetsThreshold(state.durationInfo.duration, tracked.playedMs)) {
            tracked.scrobbled = true
            event = Event.Scrobble(LastFmScrobble(track, startedAt.epochSeconds))
        }
        return event
    }

    private fun meetsThreshold(durationMs: Long, playedMs: Long): Boolean {
        if (durationMs <= MIN_TRACK_LENGTH.inWholeMilliseconds) return false
        return playedMs >= minOf(durationMs / 2, MAX_REQUIRED_PLAY.inWholeMilliseconds)
    }

    private companion object {
        val MIN_TRACK_LENGTH = 30.seconds
        val MAX_REQUIRED_PLAY = 4.minutes
        const val TICK_SLACK_MS = 1_500L
    }
}
