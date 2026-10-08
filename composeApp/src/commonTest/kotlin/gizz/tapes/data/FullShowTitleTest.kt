package gizz.tapes.data

import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

class FullShowTitleTest {

    @Test
    fun `fullShowTitle formats date and title`() {
        val title = FullShowTitle(
            title = Title("Red Rocks Amphitheatre"),
            date = LocalDate(2024, 9, 8)
        )
        assertEquals(Title("2024/9/8 Red Rocks Amphitheatre"), title.fullShowTitle)
    }

    @Test
    fun `fullShowTitle with single digit month and day`() {
        val title = FullShowTitle(
            title = Title("The Armory"),
            date = LocalDate(2024, 1, 5)
        )
        assertEquals(Title("2024/1/5 The Armory"), title.fullShowTitle)
    }

    @Test
    fun `navType escapes slashes and round trips`() {
        val title = FullShowTitle(
            title = Title("AC/DC Lane - Cherryfest - Naarm (Melbourne), VIC, Australia"),
            date = LocalDate(2012, 11, 25)
        )
        val serialized = FullShowTitle.navType.serializeAsValue(title)
        assertFalse(serialized.contains('/'))
        assertEquals(title, FullShowTitle.navType.parseValue(serialized))
    }
}
