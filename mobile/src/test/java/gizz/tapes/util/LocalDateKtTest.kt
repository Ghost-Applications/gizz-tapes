package gizz.tapes.util

import com.google.common.truth.Truth.assertThat
import kotlinx.datetime.LocalDate
import org.junit.Test

class LocalDateKtTest {
    @Test
    fun shouldFormatDate() {
        val data = LocalDate(1999, 12, 31)
        val result = data.toSimpleFormat()
        assertThat(result).isEqualTo("1999.12.31")
    }
}
