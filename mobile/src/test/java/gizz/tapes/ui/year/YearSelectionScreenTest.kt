package gizz.tapes.ui.year

import app.cash.paparazzi.Paparazzi
import gizz.tapes.noShowPlayerState
import gizz.tapes.showingPlayerState
import gizz.tapes.ui.PaparazziNightTest
import gizz.tapes.ui.PaparazziTest
import gizz.tapes.ui.player.PlayerState
import gizz.tapes.util.LCE
import gizz.tapes.yearData
import okio.IOException
import org.junit.Test

class YearSelectionScreenTest : PaparazziTest() {

    @Test
    fun error() {
        val data = LCE.Error(
            userDisplayedMessage = "There was an error getting data from Phish.in, check your network connection and try again.",
            error = IOException()
        )

        paparazzi.snapshot(data)
    }

    @Test
    fun loading() {
        paparazzi.snapshot(LCE.Loading)
    }

    @Test
    fun content() {
        paparazzi.snapshot(
            yearData = yearData
        )
    }

    @Test
    fun `content with mini player`() {
        paparazzi.snapshot(
            yearData = yearData,
            playerState = showingPlayerState
        )
    }
}

class YearSelectionScreenNightTest : PaparazziNightTest() {

    @Test
    fun error() {
        val data = LCE.Error(
            userDisplayedMessage = "There was an error getting data from Phish.in, check your network connection and try again.",
            error = IOException()
        )

        paparazzi.snapshot(data)
    }

    @Test
    fun loading() {
        paparazzi.snapshot(LCE.Loading)
    }

    @Test
    fun content() {
        paparazzi.snapshot(
            yearData = yearData
        )
    }

    @Test
    fun `content with mini player`() {
        paparazzi.snapshot(
            yearData = yearData,
            playerState = showingPlayerState
        )
    }
}

private fun Paparazzi.snapshot(
    yearData: LCE<List<YearSelectionData>, Exception>,
    playerState: PlayerState = noShowPlayerState
) {
    snapshot {
        YearSelectionScreen(
            yearData = yearData,
            playerState = playerState,
            onYearClicked = {},
            onMiniPlayerClick = {},
            onPlayAction = {},
            onPauseAction = {},
            actions = {}
        )
    }
}