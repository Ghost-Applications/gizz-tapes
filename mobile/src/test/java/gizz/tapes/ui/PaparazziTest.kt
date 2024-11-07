package gizz.tapes.ui

import app.cash.paparazzi.DeviceConfig.Companion.PIXEL_5
import app.cash.paparazzi.Paparazzi
import com.android.resources.NightMode
import org.junit.Rule

abstract class PaparazziTest {
    @get:Rule
    val paparazzi = Paparazzi(
        deviceConfig = PIXEL_5,
        theme = "Theme.Gizz"
    )
}

abstract class PaparazziNightTest {
    @get:Rule
    val paparazzi = Paparazzi(
        deviceConfig = PIXEL_5.copy(nightMode = NightMode.NIGHT),
        theme = "Theme.Gizz"
    )
}