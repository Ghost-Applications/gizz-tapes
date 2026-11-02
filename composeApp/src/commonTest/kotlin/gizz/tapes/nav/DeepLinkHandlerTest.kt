package gizz.tapes.nav

import gizz.tapes.data.ShowId
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class DeepLinkHandlerTest {

    @Test
    fun `parses show urls`() {
        assertEquals(ShowId("2026-10-17"), showIdFromUrl("https://tapes.kglw.net/2026-10-17"))
        assertEquals(ShowId("2026-08-14stu"), showIdFromUrl("https://tapes.kglw.net/2026-08-14stu"))
        assertEquals(ShowId("2023-06-08early"), showIdFromUrl("https://tapes.kglw.net/2023-06-08early/"))
        assertEquals(ShowId("2026-10-17"), showIdFromUrl("https://tapes.kglw.net/2026-10-17?ref=share"))
        assertEquals(ShowId("2026-10-17"), showIdFromUrl("https://tapes.kglw.net/2026-10-17#set-2"))
    }

    @Test
    fun `show urls round trip`() {
        val id = ShowId("2026-10-18orbrose")
        assertEquals("https://tapes.kglw.net/2026-10-18orbrose", showUrl(id))
        assertEquals(id, showIdFromUrl(showUrl(id)))
    }

    @Test
    fun `ignores non show urls`() {
        assertNull(showIdFromUrl("https://tapes.kglw.net/"))
        assertNull(showIdFromUrl("https://tapes.kglw.net/2026"))
        assertNull(showIdFromUrl("https://tapes.kglw.net/2026/random"))
        assertNull(showIdFromUrl("https://tapes.kglw.net/2026-10-17/extra"))
        assertNull(showIdFromUrl("https://example.com/2026-10-17"))
        assertNull(showIdFromUrl("http://tapes.kglw.net/2026-10-17"))
        assertNull(showIdFromUrl("gizztapes://lastfm?token=abc"))
    }

    @Test
    fun `handle stores pending show until consumed`() {
        val handler = DeepLinkHandler()
        assertFalse(handler.handle("https://tapes.kglw.net/2026"))
        assertNull(handler.pendingShow.value)

        assertTrue(handler.handle("https://tapes.kglw.net/2026-08-14stu"))
        assertEquals(ShowId("2026-08-14stu"), handler.pendingShow.value)

        handler.consume()
        assertNull(handler.pendingShow.value)
    }
}
