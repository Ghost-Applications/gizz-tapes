package gizz.tapes.lastfm

import androidx.datastore.core.DataStore
import arrow.core.left
import gizz.tapes.playback.GizzMediaPlayer
import gizz.tapes.playback.PlaybackItem
import gizz.tapes.ui.player.PlayerState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ScrobblerTest {

    private val api = FakeLastFmApiClient()
    private val auth = fakeLastFmAuthManager(api, LastFmSession("user", "sk"))
    private val queue = object : DataStore<ScrobbleQueue> {
        override val data = MutableStateFlow(ScrobbleQueue())
        override suspend fun updateData(transform: suspend (t: ScrobbleQueue) -> ScrobbleQueue): ScrobbleQueue =
            transform(data.value).also { data.value = it }
    }
    private val player = object : GizzMediaPlayer {
        override val state: StateFlow<PlayerState> = MutableStateFlow(PlayerState.NoMedia)
        override val currentPosition = 0L
        override fun setPlaylist(items: List<PlaybackItem>, startIndex: Int) = Unit
        override fun play() = Unit
        override fun pause() = Unit
        override fun seekTo(index: Int, positionMs: Long) = Unit
        override fun skipToPrevious() = Unit
        override fun skipToNext() = Unit
        override fun release() = Unit
    }
    private val scrobbler = Scrobbler(player, api, auth, queue)

    private fun scrobbles(count: Int) = List(count) {
        LastFmScrobble(LastFmTrack("King Gizzard", "Track $it", "Live", 300), timestamp = it.toLong())
    }

    @Test
    fun `flush sends oldest first in batches of 50`() = runTest {
        val queued = scrobbles(120)
        queue.data.value = ScrobbleQueue(queued)

        scrobbler.flush()

        assertEquals(listOf(50, 50, 20), api.scrobbleCalls.map { it.size })
        assertEquals(queued, api.scrobbleCalls.flatten())
        assertEquals(emptyList(), queue.data.value.items)
    }

    @Test
    fun `retryable errors keep the queue`() = runTest {
        queue.data.value = ScrobbleQueue(scrobbles(3))
        api.scrobbleResponse = { LastFmError(16, "Temporary error").left() }

        scrobbler.flush()

        assertEquals(3, queue.data.value.items.size)
    }

    @Test
    fun `network errors keep the queue`() = runTest {
        queue.data.value = ScrobbleQueue(scrobbles(3))
        api.scrobbleResponse = { IllegalStateException("offline").left() }

        scrobbler.flush()

        assertEquals(3, queue.data.value.items.size)
    }

    @Test
    fun `invalid session logs out and keeps the queue`() = runTest {
        queue.data.value = ScrobbleQueue(scrobbles(3))
        api.scrobbleResponse = { LastFmError(LastFmError.INVALID_SESSION, "Invalid session key").left() }

        scrobbler.flush()

        assertEquals(3, queue.data.value.items.size)
        assertNull(auth.session.value)
    }

    @Test
    fun `other errors drop the batch`() = runTest {
        queue.data.value = ScrobbleQueue(scrobbles(3))
        api.scrobbleResponse = { LastFmError(6, "Invalid parameters").left() }

        scrobbler.flush()

        assertEquals(emptyList(), queue.data.value.items)
    }

    @Test
    fun `nothing is sent when logged out`() = runTest {
        auth.logout()
        queue.data.value = ScrobbleQueue(scrobbles(3))

        scrobbler.flush()

        assertEquals(emptyList(), api.scrobbleCalls)
        assertEquals(3, queue.data.value.items.size)
    }
}
