package gizz.tapes.ui.player

import app.cash.paparazzi.Paparazzi
import gizz.tapes.data.Title
import gizz.tapes.noShowPlayerState
import gizz.tapes.showingPlayerState
import gizz.tapes.ui.PaparazziNightTest
import gizz.tapes.ui.PaparazziTest
import org.junit.Test

class FullPlayerTest : PaparazziTest() {

    @Test
    fun `no media`() {
        paparazzi.snapshot(noShowPlayerState)
    }

    @Test
    fun playing() {
        paparazzi.snapshot(showingPlayerState)
    }

    @Test
    fun paused() {
        paparazzi.snapshot(showingPlayerState)
    }
}

class FullPlayerNightTest : PaparazziNightTest() {

    @Test
    fun `no media`() {
        paparazzi.snapshot(noShowPlayerState)
    }

    @Test
    fun playing() {
        paparazzi.snapshot(showingPlayerState)
    }

    @Test
    fun paused() {
        paparazzi.snapshot(showingPlayerState)
    }
}

private fun Paparazzi.snapshot(state: PlayerState) {
    snapshot {
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
