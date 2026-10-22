package gizz.tapes.api.serializer

import gizz.tapes.api.data.Venue
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class HtmlEntityStringSerializerTest {

    @Test
    fun `decodes numeric and named entities`() {
        assertEquals("'s-Hertogenbosch", "&#039;s-Hertogenbosch".decodeHtmlEntities())
        assertEquals("King Gizzard & the Lizard Wizard", "King Gizzard &amp; the Lizard Wizard".decodeHtmlEntities())
        assertEquals("é 😀", "&#xE9; &#128512;".decodeHtmlEntities())
    }

    @Test
    fun `leaves plain text and unknown entities alone`() {
        assertEquals("Rock & Roll", "Rock & Roll".decodeHtmlEntities())
        assertEquals("&bogus;", "&bogus;".decodeHtmlEntities())
    }

    @Test
    fun `decodes venue fields including null region`() {
        val venue = Json.decodeFromString<Venue>(
            """{"id":625,"slug":"x","name":"MAINSTAGE","city":"&#039;s-Hertogenbosch","region":null,"country_id":10,"show_count":1}"""
        )
        assertEquals("'s-Hertogenbosch", venue.city)
        assertNull(venue.region)
    }
}
