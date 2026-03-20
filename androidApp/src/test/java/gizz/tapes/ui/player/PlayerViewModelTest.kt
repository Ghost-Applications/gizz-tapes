package gizz.tapes.ui.player

import com.google.common.truth.Truth.assertThat
import gizz.tapes.playback.GizzMediaPlayer
import gizz.tapes.playback.PlaybackItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class PlayerViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `playerState reflects media player state`() {
        val expectedState = PlayerState.NoMedia
        val classUnderTest = PlayerViewModel(FakeGizzMediaPlayer(expectedState))

        assertThat(classUnderTest.playerState.value).isEqualTo(expectedState)
    }

    @Test
    fun `playerState updates when media player state changes`() {
        val fakePlayer = FakeGizzMediaPlayer(PlayerState.NoMedia)
        val classUnderTest = PlayerViewModel(fakePlayer)

        val newState = PlayerState.MediaLoaded(
            isPlaying = true,
            isLoading = false,
            currentTrackIndex = 0,
            showId = gizz.tapes.data.ShowId("test-show"),
            showTitle = gizz.tapes.data.FullShowTitle(
                title = gizz.tapes.data.Title("Test Show"),
                date = kotlinx.datetime.LocalDate(2024, 1, 1)
            ),
            durationInfo = MediaDurationInfo(currentPosition = 0L, duration = 60_000L),
            artworkUri = null,
            title = "Test Track",
            albumTitle = "Test Album",
            mediaId = "test-id"
        )
        fakePlayer.stateFlow.value = newState

        assertThat(classUnderTest.playerState.value).isEqualTo(newState)
    }

    @Test
    fun `play delegates to media player`() {
        val fakePlayer = FakeGizzMediaPlayer(PlayerState.NoMedia)
        val classUnderTest = PlayerViewModel(fakePlayer)

        classUnderTest.play()

        assertThat(fakePlayer.playCalled).isTrue()
    }

    @Test
    fun `pause delegates to media player`() {
        val fakePlayer = FakeGizzMediaPlayer(PlayerState.NoMedia)
        val classUnderTest = PlayerViewModel(fakePlayer)

        classUnderTest.pause()

        assertThat(fakePlayer.pauseCalled).isTrue()
    }

    @Test
    fun `seekTo delegates to media player`() {
        val fakePlayer = FakeGizzMediaPlayer(PlayerState.NoMedia)
        val classUnderTest = PlayerViewModel(fakePlayer)

        classUnderTest.seekTo(2, 5000L)

        assertThat(fakePlayer.seekToIndex).isEqualTo(2)
        assertThat(fakePlayer.seekToPosition).isEqualTo(5000L)
    }

    @Test
    fun `skipToPrevious delegates to media player`() {
        val fakePlayer = FakeGizzMediaPlayer(PlayerState.NoMedia)
        val classUnderTest = PlayerViewModel(fakePlayer)

        classUnderTest.skipToPrevious()

        assertThat(fakePlayer.skipToPreviousCalled).isTrue()
    }

    @Test
    fun `skipToNext delegates to media player`() {
        val fakePlayer = FakeGizzMediaPlayer(PlayerState.NoMedia)
        val classUnderTest = PlayerViewModel(fakePlayer)

        classUnderTest.skipToNext()

        assertThat(fakePlayer.skipToNextCalled).isTrue()
    }
}

private class FakeGizzMediaPlayer(initialState: PlayerState) : GizzMediaPlayer {
    val stateFlow = MutableStateFlow(initialState)
    override val state: StateFlow<PlayerState> = stateFlow
    override val currentPosition: Long = 0L

    var playCalled = false
    var pauseCalled = false
    var seekToIndex: Int? = null
    var seekToPosition: Long? = null
    var skipToPreviousCalled = false
    var skipToNextCalled = false

    override fun setPlaylist(items: List<PlaybackItem>, startIndex: Int) = Unit
    override fun play() { playCalled = true }
    override fun pause() { pauseCalled = true }
    override fun seekTo(index: Int, positionMs: Long) {
        seekToIndex = index
        seekToPosition = positionMs
    }
    override fun skipToPrevious() { skipToPreviousCalled = true }
    override fun skipToNext() { skipToNextCalled = true }
    override fun release() = Unit
}
