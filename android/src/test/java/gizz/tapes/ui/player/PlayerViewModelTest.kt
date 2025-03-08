package gizz.tapes.ui.player

import androidx.lifecycle.SavedStateHandle
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import com.google.common.truth.Truth.assertThat
import gizz.tapes.MainDispatcherRule
import gizz.tapes.data.FullShowTitle
import gizz.tapes.data.PlayerErrorMessage
import gizz.tapes.data.Title
import gizz.tapes.mediaItem
import gizz.tapes.playback.MediaPlayerContainer
import gizz.tapes.stub
import kotlinx.datetime.LocalDate
import kotlinx.serialization.json.Json
import org.junit.Rule
import org.junit.Test

class PlayerViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `title should be null when one is not in savedStateHandle`() {
        val classUnderTest = PlayerViewModel(
            mediaPlayerContainer = unimportantMediaPlayerContainer,
            savedStateHandle = SavedStateHandle(),
            playerErrorMessage = PlayerErrorMessage("There was an error!")
        )

        assertThat(classUnderTest.title).isNull()
    }

    @Test
    fun `title should be returned from SavedStateHandle`() {
        val expectedShowTitle = FullShowTitle(
            title = Title("Alpine Valley"),
            date = LocalDate(2025, 1, 1)
        )

        val classUnderTest = PlayerViewModel(
            mediaPlayerContainer = unimportantMediaPlayerContainer,
            savedStateHandle = SavedStateHandle(
                initialState = mapOf(
                    "showTitle" to Json.encodeToString(expectedShowTitle)
                )
            ),
            playerErrorMessage = PlayerErrorMessage("There was an error!")
        )

        assertThat(classUnderTest.title).isEqualTo(expectedShowTitle)
    }
}

private val unimportantMediaPlayerContainer = object : MediaPlayerContainer {
    override val mediaPlayer: Player = object : Player by stub() {
        override fun addListener(listener: Player.Listener) = Unit
        override fun getCurrentMediaItem(): MediaItem = mediaItem
        override fun isPlaying(): Boolean = false
        override fun getCurrentPosition(): Long = 1000L
        override fun getDuration(): Long = 1000 * 4 * 60L
        override fun getContentPosition(): Long = 1000 * 10
    }
}

