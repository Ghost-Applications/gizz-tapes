package gizz.tapes.ui.show

import gizz.tapes.data.Title
import gizz.tapes.showContent
import gizz.tapes.showingPlayerState
import gizz.tapes.ui.PaparazziTest
import gizz.tapes.util.LCE
import okio.IOException
import org.junit.Test

class ShowScreenTest : PaparazziTest() {
    @Test
    fun error() {
        snapshot(
            state = LCE.Error(
                userDisplayedMessage = "There was an error getting data from Phish.in, check your network connection and try again.",
                error = IOException("An error occurred")
            )
        )
    }

    @Test
    fun loading() {
        snapshot(state = LCE.Loading)
    }

    @Test
    fun content() {
        snapshot(state = showContent)
    }

    private fun snapshot(
        state: LCE<ShowScreenData, Exception>,
    ) {
        paparazzi.snapshot {
            ShowScreen(
                state = state,
                playerState = showingPlayerState,
                appBarTitle = Title("2021/08/08 Ruoff Music Center"),
                onMiniPlayerClick = {},
                onPauseAction = {},
                onPlayAction = {},
                actions = {},
                onRowClick = { _, _ -> },
                upClick = {}
            )
        }
    }
}