package gizz.tapes.util

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class LCETest {

    // contentOrNull

    @Test
    fun `contentOrNull returns value for Content`() {
        assertEquals("hello", LCE.Content("hello").contentOrNull())
    }

    @Test
    fun `contentOrNull returns null for Loading`() {
        assertNull(LCE.Loading.contentOrNull<String, Nothing>())
    }

    @Test
    fun `contentOrNull returns null for Error`() {
        assertNull(LCE.Error(Exception()).contentOrNull())
    }

    // onContent

    @Test
    fun `onContent invokes action for Content`() {
        var called = false
        LCE.Content("value").onContent { called = true }
        assertEquals(true, called)
    }

    @Test
    fun `onContent does not invoke action for Loading`() {
        var called = false
        LCE.Loading.onContent<String, Nothing> { called = true }
        assertEquals(false, called)
    }

    @Test
    fun `onContent does not invoke action for Error`() {
        var called = false
        LCE.Error(Exception()).onContent { called = true }
        assertEquals(false, called)
    }

    // map

    @Test
    fun `map transforms Content value`() {
        assertEquals(LCE.Content(42), LCE.Content("42").map { it.toInt() })
    }

    @Test
    fun `map passes through Loading`() {
        val result = LCE.Loading.map<String, String, Nothing> { "transformed" }
        assertEquals(LCE.Loading, result)
    }

    @Test
    fun `map passes through Error`() {
        val error = LCE.Error(Exception())
        val result = error.map { "transformed" }
        assertEquals(error, result)
    }

    // mapCollection

    @Test
    fun `mapCollection transforms each element in Content`() {
        val result = LCE.Content(listOf("1", "2", "3")).mapCollection { it.toInt() }
        assertEquals(LCE.Content(listOf(1, 2, 3)), result)
    }

    @Test
    fun `mapCollection passes through Loading`() {
        val result = LCE.Loading.mapCollection<String, String, Nothing> { "x" }
        assertEquals(LCE.Loading, result)
    }

    @Test
    fun `mapCollection passes through Error`() {
        val error: LCE.Error<Exception> = LCE.Error(Exception())
        val result = error.mapCollection<String, String, Exception> { "x" }
        assertEquals(error, result)
    }
}
