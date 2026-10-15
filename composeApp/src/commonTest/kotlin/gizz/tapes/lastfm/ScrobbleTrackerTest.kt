package gizz.tapes.lastfm

import gizz.tapes.data.FullShowTitle
import gizz.tapes.data.ShowId
import gizz.tapes.data.Title
import gizz.tapes.ui.player.MediaDurationInfo
import gizz.tapes.ui.player.PlayerState
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.time.Clock
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant

class ScrobbleTrackerTest {

    private val clock = object : Clock {
        var now = Instant.fromEpochSeconds(1_000_000)
        override fun now() = now
    }
    private val tracker = ScrobbleTracker(clock)

    private fun state(
        position: Duration,
        duration: Duration,
        mediaId: String = "track-1",
        isPlaying: Boolean = true,
    ) = PlayerState.MediaLoaded(
        isPlaying = isPlaying,
        isLoading = false,
        showId = ShowId("show"),
        showTitle = FullShowTitle(Title("Show"), LocalDate(2024, 9, 11)),
        durationInfo = MediaDurationInfo(position.inWholeMilliseconds, duration.inWholeMilliseconds),
        artworkUri = null,
        title = "Rattlesnake",
        albumTitle = "Live",
        mediaId = mediaId,
        currentTrackIndex = 0,
        artistName = "King Gizzard",
    )

    /** Plays from [from] to [to] in 1s ticks, returning every emitted event. */
    private fun play(
        from: Duration,
        to: Duration,
        duration: Duration,
        mediaId: String = "track-1",
    ): List<ScrobbleTracker.Event> {
        val events = mutableListOf<ScrobbleTracker.Event>()
        var position = from
        while (position <= to) {
            tracker.onState(state(position, duration, mediaId))?.let(events::add)
            clock.now += 1.seconds
            position += 1.seconds
        }
        return events
    }

    @Test
    fun `sends now playing when a track starts`() {
        val event = tracker.onState(state(0.seconds, 3.minutes))

        assertEquals(
            ScrobbleTracker.Event.NowPlaying(LastFmTrack("King Gizzard", "Rattlesnake", "Live", 180)),
            event
        )
    }

    @Test
    fun `scrobbles after half the track with the start timestamp`() {
        val startedAt = clock.now
        val events = play(0.seconds, 90.seconds, duration = 3.minutes)

        val scrobble = assertIs<ScrobbleTracker.Event.Scrobble>(events.last())
        assertEquals(startedAt.epochSeconds, scrobble.scrobble.timestamp)
        assertEquals(2, events.size)
    }

    @Test
    fun `does not scrobble before half the track`() {
        val events = play(0.seconds, 89.seconds, duration = 3.minutes)

        assertEquals(1, events.size)
    }

    @Test
    fun `long tracks scrobble after four minutes`() {
        val events = play(0.seconds, 4.minutes, duration = 20.minutes)

        assertIs<ScrobbleTracker.Event.Scrobble>(events.last())
    }

    @Test
    fun `tracks of 30 seconds or less are never scrobbled`() {
        val events = play(0.seconds, 30.seconds, duration = 30.seconds)

        assertEquals(
            listOf<Any>(ScrobbleTracker.Event.NowPlaying(LastFmTrack("King Gizzard", "Rattlesnake", "Live", 30))),
            events
        )
    }

    @Test
    fun `seeking ahead does not count as listening`() {
        play(0.seconds, 10.seconds, duration = 3.minutes)
        val events = play(170.seconds, 180.seconds, duration = 3.minutes)

        assertEquals(emptyList(), events)
    }

    @Test
    fun `paused time does not count`() {
        play(0.seconds, 60.seconds, duration = 3.minutes)
        tracker.onState(state(60.seconds, 3.minutes, isPlaying = false))
        clock.now += 10.minutes

        assertNull(tracker.onState(state(60.seconds, 3.minutes)))
    }

    @Test
    fun `scrobbles only once per play`() {
        val events = play(0.seconds, 180.seconds, duration = 3.minutes)

        assertEquals(1, events.count { it is ScrobbleTracker.Event.Scrobble })
    }

    @Test
    fun `a new track resets progress`() {
        play(0.seconds, 80.seconds, duration = 3.minutes)
        val events = play(0.seconds, 20.seconds, duration = 3.minutes, mediaId = "track-2")

        assertEquals(1, events.size)
        assertIs<ScrobbleTracker.Event.NowPlaying>(events.single())
    }

    @Test
    fun `no media resets so replaying the same track counts again`() {
        play(0.seconds, 180.seconds, duration = 3.minutes)
        tracker.onState(PlayerState.NoMedia)
        val events = play(0.seconds, 90.seconds, duration = 3.minutes)

        assertEquals(1, events.count { it is ScrobbleTracker.Event.Scrobble })
    }
}
