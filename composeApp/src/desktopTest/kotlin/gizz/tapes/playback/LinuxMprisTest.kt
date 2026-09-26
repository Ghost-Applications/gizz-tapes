package gizz.tapes.playback

import gizz.tapes.data.FullShowTitle
import gizz.tapes.data.ShowId
import gizz.tapes.data.Title
import gizz.tapes.ui.player.MediaDurationInfo
import gizz.tapes.ui.player.PlayerState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.datetime.LocalDate
import org.freedesktop.dbus.DBusPath
import org.freedesktop.dbus.exceptions.DBusExecutionException
import org.freedesktop.dbus.types.Variant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class LinuxMprisTest {

    private val player = FakePlayer()
    private val mpris = LinuxMpris(player)

    @Test
    fun `no media reports stopped with empty metadata`() {
        val properties = playerProperties()

        assertEquals("Stopped", properties.value("PlaybackStatus"))
        assertEquals(emptyMap<String, Variant<*>>(), properties.value("Metadata"))
        assertFalse(properties.value("CanPlay") as Boolean)
        assertTrue(properties.value("CanControl") as Boolean)
    }

    @Test
    fun `playing track reports status, metadata and position in microseconds`() {
        player.state.value = mediaLoaded(isPlaying = true, artworkUri = "https://example.com/art.png")
        player.currentPosition = 1_500L

        val properties = playerProperties()
        val metadata = properties.metadata()

        assertEquals("Playing", properties.value("PlaybackStatus"))
        assertEquals(1_500_000L, properties.value("Position"))
        assertEquals(DBusPath("/gizz/tapes/track/2"), metadata.value("mpris:trackid"))
        assertEquals(60_000_000L, metadata.value("mpris:length"))
        assertEquals("Track", metadata.value("xesam:title"))
        assertEquals("Album", metadata.value("xesam:album"))
        assertEquals("https://example.com/art.png", metadata.value("mpris:artUrl"))
    }

    @Test
    fun `paused track reports paused`() {
        player.state.value = mediaLoaded(isPlaying = false)

        assertEquals("Paused", playerProperties().value("PlaybackStatus"))
    }

    @Test
    fun `bundled jar artwork is not exposed`() {
        player.state.value = mediaLoaded(artworkUri = "jar:file:/app.jar!/logo.png")

        assertNull(playerProperties().metadata()["mpris:artUrl"])
    }

    @Test
    fun `root interface identifies the app`() {
        assertEquals("Gizz Tapes", mpris.GetAll("org.mpris.MediaPlayer2").value("Identity"))
    }

    @Test
    fun `Get returns a single property and rejects unknown ones`() {
        player.state.value = mediaLoaded(isPlaying = true)

        assertEquals("Playing", mpris.Get<Variant<*>>(PLAYER, "PlaybackStatus").value)
        assertFailsWith<DBusExecutionException> { mpris.Get<Variant<*>>(PLAYER, "Nope") }
    }

    @Test
    fun `playPause toggles based on current state`() {
        player.state.value = mediaLoaded(isPlaying = true)
        mpris.playPause()
        player.state.value = mediaLoaded(isPlaying = false)
        mpris.playPause()

        assertEquals(listOf("pause", "play"), player.calls)
    }

    @Test
    fun `seek is relative and clamped to the track`() {
        player.state.value = mediaLoaded()
        player.currentPosition = 10_000L

        mpris.seek(5_000_000L)
        mpris.seek(-60_000_000L)
        mpris.seek(120_000_000L)

        assertEquals(listOf("seekTo 2 15000", "seekTo 2 0", "seekTo 2 60000"), player.calls)
    }

    @Test
    fun `setPosition ignores a track that is no longer current`() {
        player.state.value = mediaLoaded()

        mpris.setPosition(DBusPath("/gizz/tapes/track/1"), 5_000_000L)
        mpris.setPosition(DBusPath("/gizz/tapes/track/2"), 5_000_000L)

        assertEquals(listOf("seekTo 2 5000"), player.calls)
    }

    private fun playerProperties() = mpris.GetAll(PLAYER)

    @Suppress("UNCHECKED_CAST")
    private fun Map<String, Variant<*>>.metadata() = value("Metadata") as Map<String, Variant<*>>

    private fun Map<String, Variant<*>>.value(key: String) = getValue(key).value

    private fun mediaLoaded(isPlaying: Boolean = false, artworkUri: String? = null) = PlayerState.MediaLoaded(
        isPlaying = isPlaying,
        isLoading = false,
        showId = ShowId("show-1"),
        showTitle = FullShowTitle(Title("Test Show"), LocalDate(2024, 1, 1)),
        durationInfo = MediaDurationInfo(0L, 60_000L),
        artworkUri = artworkUri,
        title = "Track",
        albumTitle = "Album",
        mediaId = "media-1",
        currentTrackIndex = 2,
    )

    private class FakePlayer : GizzMediaPlayer {
        override val state = MutableStateFlow<PlayerState>(PlayerState.NoMedia)
        override var currentPosition = 0L
        val calls = mutableListOf<String>()

        override fun setPlaylist(items: List<PlaybackItem>, startIndex: Int) = Unit
        override fun play() {
            calls += "play"
        }
        override fun pause() {
            calls += "pause"
        }
        override fun seekTo(index: Int, positionMs: Long) {
            calls += "seekTo $index $positionMs"
        }
        override fun skipToPrevious() {
            calls += "previous"
        }
        override fun skipToNext() {
            calls += "next"
        }
        override fun release() = Unit
    }

    private companion object {
        const val PLAYER = "org.mpris.MediaPlayer2.Player"
    }
}
