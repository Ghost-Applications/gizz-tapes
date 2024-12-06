package gizz.tapes.ui.show

import app.cash.paparazzi.Paparazzi
import gizz.tapes.data.Title
import gizz.tapes.showContent
import gizz.tapes.showingPlayerState
import gizz.tapes.ui.PaparazziNightTest
import gizz.tapes.ui.PaparazziTest
import gizz.tapes.util.LCE
import okio.IOException
import org.junit.Test

class ShowScreenTest : PaparazziTest() {
    @Test
    fun error() {
        paparazzi.snapshot(
            state = LCE.Error(
                userDisplayedMessage = "There was an error getting data from Phish.in, check your network connection and try again.",
                error = IOException("An error occurred")
            )
        )
    }

    @Test
    fun loading() {
        paparazzi.snapshot(state = LCE.Loading)
    }

    @Test
    fun content() {
        paparazzi.snapshot(state = showContent)
    }
}

class ShowScreenNightTest : PaparazziNightTest() {
    @Test
    fun error() {
        paparazzi.snapshot(
            state = LCE.Error(
                userDisplayedMessage = "There was an error getting data from Phish.in, check your network connection and try again.",
                error = IOException("An error occurred")
            )
        )
    }

    @Test
    fun loading() {
        paparazzi.snapshot(state = LCE.Loading)
    }

    @Test
    fun content() {
        paparazzi.snapshot(state = showContent)
    }
}

private fun Paparazzi.snapshot(
    state: LCE<ShowScreenState, Exception>,
) {
    snapshot {
        ShowScreen(
            state = state,
            playerState = showingPlayerState,
            appBarTitle = Title("2021/08/08 Ruoff Music Center"),
            onMiniPlayerClick = {},
            onPauseAction = {},
            onPlayAction = {},
            actions = {},
            onRowClick = { _, _ -> },
            navigateUp = {}
        )
    }
}
