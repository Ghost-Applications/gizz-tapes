package gizz.tapes.ui.show

import app.cash.paparazzi.Paparazzi
import gizz.tapes.data.Title
import gizz.tapes.noShowPlayerState
import gizz.tapes.showListContent
import gizz.tapes.showingPlayerState
import gizz.tapes.ui.PaparazziNightTest
import gizz.tapes.ui.PaparazziTest
import gizz.tapes.ui.player.PlayerState
import gizz.tapes.util.LCE
import okio.IOException
import org.junit.Test

class ShowSelectionScreenTest : PaparazziTest() {

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
        paparazzi.snapshot(state = showListContent)
    }

    @Test
    fun `content with mini player`() {
        paparazzi.snapshot(
            state = showListContent,
            playerState = showingPlayerState
        )
    }
}

class ShowSelectionScreenNightTest : PaparazziNightTest() {

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
        paparazzi.snapshot(state = showListContent)
    }

    @Test
    fun `content with mini player`() {
        paparazzi.snapshot(
            state = showListContent,
            playerState = showingPlayerState
        )
    }
}

private fun Paparazzi.snapshot(
    state: LCE<List<ShowSelectionData>, Exception>,
    playerState: PlayerState = noShowPlayerState
) {
    snapshot {
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