package gizz.tapes.ui.player

import gizz.tapes.data.Title
import gizz.tapes.noShowPlayerState
import gizz.tapes.showingPlayerState
import gizz.tapes.ui.PaparazziTest
import org.junit.Test

class FullPlayerTest : PaparazziTest() {

    @Test
    fun `no media`() {
        snapshot(noShowPlayerState)
    }

    @Test
    fun playing() {
        snapshot(showingPlayerState)
    }

    @Test
    fun paused() {
        snapshot(showingPlayerState.copy(isPlaying = false))
    }

    private fun snapshot(state: PlayerState) {
        paparazzi.snapshot {
            FullPlayer(
                playerState = state,
                title = Title("2021/08/08 Deer Creek Music Center"),
                navigateToShow = { _, _ -> },
                upClick = { },
                seekTo = {},
                seekToPreviousMediaItem = { },
                seekToNextMediaItem = { },
                onPause = {},
                onPlay = {},
                actions = {}
            )
        }
    }
}