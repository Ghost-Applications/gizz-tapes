package gizz.tapes.util

import gizz.tapes.api.data.PartialShowData
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals

class PartialShowDataKtTest {
    @Test
    fun `should return formatted venueName location when title is empty`() {
        val partialShowData = PartialShowData(
            id = "2024-09-03",
            date = LocalDate.parse("2024-09-03"),
            venueName = "The Armory",
            location = "Minneapolis, MN, USA",
            title = "",
            order = 1.toUShort(),
            posterUrl = "https://kglw.net/i/poster-art-1699403231.jpeg",
            averageRating = 1f,
            countRatings = 1f,
            weightedRating = 1f,
            tags = emptyList(),
            artistName = "King Gizzard & The Lizard Wizard",
        )

        assertEquals("The Armory • Minneapolis, MN, USA", partialShowData.showTitle)
    }

    @Test
    fun `should return formatted venueName location when title is null`() {
        val partialShowData = PartialShowData(
            id = "2024-09-03",
            date = LocalDate.parse("2024-09-03"),
            venueName = "The Armory",
            location = "Minneapolis, MN, USA",
            title = null,
            order = 1.toUShort(),
            posterUrl = "https://kglw.net/i/poster-art-1699403231.jpeg",
            averageRating = null,
            countRatings = 0f,
            weightedRating = null,
            tags = emptyList(),
            artistName = "King Gizzard & The Lizard Wizard",
        )

        assertEquals("The Armory • Minneapolis, MN, USA", partialShowData.showTitle)
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
            posterUrl = "https://kglw.net/i/poster-art-1699403231.jpeg",
            averageRating = null,
            countRatings = 0f,
            weightedRating = null,
            tags = emptyList(),
            artistName = "King Gizzard & The Lizard Wizard",
        )

        assertEquals(
            "The Armory • Greatest Show Ever • Minneapolis, MN, USA",
            partialShowData.showTitle
        )
    }

    @Test
    fun `should drop main band name regardless of case`() {
        val partialShowData = PartialShowData(
            id = "2024-09-03",
            date = LocalDate.parse("2024-09-03"),
            venueName = "The Armory",
            location = "Minneapolis, MN, USA",
            title = null,
            order = 1.toUShort(),
            posterUrl = null,
            averageRating = null,
            countRatings = 0f,
            weightedRating = null,
            tags = emptyList(),
            artistName = "King Gizzard & the Lizard Wizard",
        )

        assertEquals("The Armory • Minneapolis, MN, USA", partialShowData.showTitle)
    }

    @Test
    fun `should include artistName when not the main band`() {
        val partialShowData = PartialShowData(
            id = "2025-01-01",
            date = LocalDate.parse("2025-01-01"),
            venueName = "Bourke Street Mall",
            location = "Naarm (Melbourne), VIC, Australia",
            title = null,
            order = 1.toUShort(),
            posterUrl = null,
            averageRating = null,
            countRatings = 0f,
            weightedRating = null,
            tags = emptyList(),
            artistName = "Stu Mackenzie",
        )

        assertEquals(
            "Stu Mackenzie • Bourke Street Mall • Naarm (Melbourne), VIC, Australia",
            partialShowData.showTitle
        )
    }
}
