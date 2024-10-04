package gizz.tapes.ui.show

import gizz.tapes.data.Title
import gizz.tapes.noShowPlayerState
import gizz.tapes.showListContent
import gizz.tapes.showingPlayerState
import gizz.tapes.ui.PaparazziTest
import gizz.tapes.ui.player.PlayerState
import gizz.tapes.util.LCE
import okio.IOException
import org.junit.Test

class ShowSelectionScreenTest : PaparazziTest() {

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
        snapshot(state = showListContent)
    }

    @Test
    fun `content with mini player`() {
        snapshot(
            state = showListContent,
            playerState = showingPlayerState
        )
    }

    private fun snapshot(
        state: LCE<List<ShowSelectionData>, Exception>,
        playerState: PlayerState = noShowPlayerState
    ) {
        paparazzi.snapshot {
            ShowSelectionScreen(
                screenTitle = Title("2001"),
                state = state,
                playerState = playerState,
                navigateUpClick = {},
                onShowClicked = { _, _ -> },
                onMiniPlayerClick = {},
                onPauseAction = {},
                onPlayAction = {},
                actions = {}
            )
        }
    }
}