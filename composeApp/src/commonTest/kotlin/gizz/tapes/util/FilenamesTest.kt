package gizz.tapes.util

import kotlin.test.Test
import kotlin.test.assertEquals

class FilenamesTest {

    // sanitizeFileName

    @Test
    fun `sanitizeFileName leaves an ordinary name untouched`() {
        assertEquals("My Show Title", sanitizeFileName("My Show Title"))
    }

    @Test
    fun `sanitizeFileName replaces path separators and reserved characters`() {
        assertEquals("a_b_c_d_e_f_g_h_i", sanitizeFileName("""a/b\c:d*e?f"g<h>i"""))
    }

    @Test
    fun `sanitizeFileName trims surrounding whitespace`() {
        assertEquals("Show", sanitizeFileName("  Show  "))
    }

    @Test
    fun `sanitizeFileName replaces a bare parent directory reference`() {
        assertEquals("_", sanitizeFileName(".."))
    }

    @Test
    fun `sanitizeFileName replaces a bare current directory reference`() {
        assertEquals("_", sanitizeFileName("."))
    }

    @Test
    fun `sanitizeFileName replaces an empty string`() {
        assertEquals("_", sanitizeFileName(""))
    }

    @Test
    fun `sanitizeFileName replaces a string that is only whitespace`() {
        assertEquals("_", sanitizeFileName("   "))
    }

    @Test
    fun `sanitizeFileName does not flag a name that merely contains dots`() {
        assertEquals("01-Intro.mp3", sanitizeFileName("01-Intro.mp3"))
    }

    @Test
    fun `sanitizeFileName does not turn a traversal attempt back into one via separator stripping`() {
        // "../../etc" has its slashes replaced, so it can no longer walk out of a directory
        // when used as a single path segment (e.g. ShowSaver.localPath()).
        assertEquals(".._.._etc", sanitizeFileName("../../etc"))
    }
}
