package gizz.tapes.util

import com.google.common.truth.Truth.assertThat
import gizz.tapes.api.data.PartialShowData
import kotlinx.datetime.LocalDate
import org.junit.Test

class PartialShowDataKtTest {
    @Test
    fun `should return formatted venueName location, when title is empty`() {
        val partialShowData = PartialShowData(
            id = "2024-09-03",
            date = LocalDate.parse("2024-09-03"),
            venueName = "The Armory",
            location = "Minneapolis, MN, USA",
            title = "",
            order = 1.toUShort(),
            posterUrl = "https://kglw.net/i/poster-art-1699403231.jpeg"
        )

        assertThat(partialShowData.showTitle).isEqualTo("The Armory - Minneapolis, MN, USA")
    }

    @Test
    fun `should return formatted venueName location, when title is null`() {
        val partialShowData = PartialShowData(
            id = "2024-09-03",
            date = LocalDate.parse("2024-09-03"),
            venueName = "The Armory",
            location = "Minneapolis, MN, USA",
            title = null,
            order = 1.toUShort(),
            posterUrl = "https://kglw.net/i/poster-art-1699403231.jpeg"
        )

        assertThat(partialShowData.showTitle).isEqualTo("The Armory - Minneapolis, MN, USA")
    }

    @Test
    fun `should return formatted venueName title location`() {
        val partialShowData = PartialShowData(
            id = "2024-09-03",
            date = LocalDate.parse("2024-09-03"),
            venueName = "The Armory",
            location = "Minneapolis, MN, USA",
            title = "Greatest Show Ever",
            order = 1.toUShort(),
            posterUrl = "https://kglw.net/i/poster-art-1699403231.jpeg"
        )

        assertThat(partialShowData.showTitle).isEqualTo("The Armory - Greatest Show Ever - Minneapolis, MN, USA")
    }
}
