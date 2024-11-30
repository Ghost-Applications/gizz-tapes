package gizz.tapes.ui.menu

import gizz.tapes.ui.PaparazziNightTest
import gizz.tapes.ui.PaparazziTest
import org.junit.Test

class AboutScreenTest : PaparazziTest() {
    @Test
    fun `about screen`() {
        paparazzi.snapshot {
            AboutScreen { }
        }
    }
}

class AboutScreenNightTest : PaparazziNightTest() {
    @Test
    fun `about screen`() {
        paparazzi.snapshot {
            AboutScreen { }
        }
    }
}
