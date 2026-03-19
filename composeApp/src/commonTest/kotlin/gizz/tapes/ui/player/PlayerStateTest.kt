package gizz.tapes.ui.player

import gizz.tapes.data.FullShowTitle
import gizz.tapes.data.ShowId
import gizz.tapes.data.Title
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertIs

class PlayerStateTest {

    private val dummyState = PlayerState.MediaLoaded(
        isPlaying = false,
        isLoading = false,
        currentTrackIndex = 0,
        showId = ShowId("show-1"),
        showTitle = FullShowTitle(Title("Test Show"), LocalDate(2024, 1, 1)),
        durationInfo = MediaDurationInfo(0L, 60_000L),
        artworkUri = null,
        title = "Track",
        albumTitle = "Album",
        mediaId = "media-1"
    )

    @Test
    fun `invoke returns Playing when isPlaying is true`() {
        val result = PlayerState.MediaLoaded(
            isPlaying = true,
            isLoading = false,
            currentTrackIndex = 0,
            showId = ShowId("show-1"),
            showTitle = FullShowTitle(Title("Test Show"), LocalDate(2024, 1, 1)),
            durationInfo = MediaDurationInfo.Empty,
            artworkUri = null,
            title = "Track",
            albumTitle = "Album",
            mediaId = "media-1"
        )
        assertIs<PlayerState.MediaLoaded.Playing>(result)
    }

    @Test
    fun `invoke returns Loading when isLoading is true`() {
        val result = PlayerState.MediaLoaded(
            isPlaying = false,
            isLoading = true,
            currentTrackIndex = 0,
            showId = ShowId("show-1"),
            showTitle = FullShowTitle(Title("Test Show"), LocalDate(2024, 1, 1)),
            durationInfo = MediaDurationInfo.Empty,
            artworkUri = null,
            title = "Track",
            albumTitle = "Album",
            mediaId = "media-1"
        )
        assertIs<PlayerState.MediaLoaded.Loading>(result)
    }

    @Test
    fun `invoke returns Paused when isPlaying and isLoading are false`() {
        val result = PlayerState.MediaLoaded(
            isPlaying = false,
            isLoading = false,
            currentTrackIndex = 0,
            showId = ShowId("show-1"),
            showTitle = FullShowTitle(Title("Test Show"), LocalDate(2024, 1, 1)),
            durationInfo = MediaDurationInfo.Empty,
            artworkUri = null,
            title = "Track",
            albumTitle = "Album",
            mediaId = "media-1"
        )
        assertIs<PlayerState.MediaLoaded.Paused>(result)
    }

    @Test
    fun `copy with isPlaying true returns Playing`() {
        assertIs<PlayerState.MediaLoaded.Playing>(dummyState.copy(isPlaying = true))
    }

    @Test
    fun `copy with isPlaying false returns Paused`() {
        assertIs<PlayerState.MediaLoaded.Paused>(dummyState.copy(isPlaying = false))
    }
}
