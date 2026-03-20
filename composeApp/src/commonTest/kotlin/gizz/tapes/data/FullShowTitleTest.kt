package gizz.tapes.data

import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals

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
}
