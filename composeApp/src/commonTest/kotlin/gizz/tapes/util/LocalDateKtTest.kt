package gizz.tapes.util

import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals

class LocalDateKtTest {
    @Test
    fun shouldFormatDate() {
        val data = LocalDate(1999, 12, 31)
        val result = data.toSimpleFormat()
        assertEquals("1999.12.31", result)
    }

    @Test
    fun shouldFormatDateAsAlbumFormat() {
        val data = LocalDate(1999, 12, 31)
        val result = data.toAlbumFormat()
        assertEquals("1999/12/31", result)
    }
}
