package gizz.tapes.ui.year

import gizz.tapes.noShowPlayerState
import gizz.tapes.showingPlayerState
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

        snapshot(data)
    }

    @Test
    fun loading() {
        snapshot(LCE.Loading)
    }

    @Test
    fun content() {
        snapshot(
            yearData = yearData
        )
    }

    @Test
    fun `content with mini player`() {
        snapshot(
            yearData = yearData,
            playerState = showingPlayerState
        )
    }

    private fun snapshot(
        yearData: LCE<List<YearSelectionData>, Exception>,
        playerState: PlayerState = noShowPlayerState
    ) {
        paparazzi.snapshot {
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
}