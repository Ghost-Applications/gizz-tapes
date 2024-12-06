package gizz.tapes.ui.menu.settings

import app.cash.paparazzi.Paparazzi
import gizz.tapes.api.data.Recording
import gizz.tapes.ui.PaparazziNightTest
import gizz.tapes.ui.PaparazziTest
import gizz.tapes.util.LCE
import org.junit.Test

interface SettingsScreenTest {
    val paparazzi: Paparazzi

    fun runTest(state: LCE<SettingsScreenState, Nothing>) {
        paparazzi.snapshot {
            SettingsScreen(
                state = state,
                onRecordingTypeSelected = { },
                navigateUp = { }
            )
        }
    }
}

class SettingsScreenDayTest : PaparazziTest(), SettingsScreenTest {
    @Test
    fun content() {
        runTest(
            LCE.Content(
                SettingsScreenState(
                    selectedPreferredRecordingType = Recording.Type.SBD
                )
            )
        )
    }
}

class SettingsScreenNightTest : PaparazziNightTest(), SettingsScreenTest {
    @Test
    fun content() {
        runTest(
            LCE.Content(
                SettingsScreenState(
                    selectedPreferredRecordingType = Recording.Type.SBD
                )
            )
        )
    }
}
