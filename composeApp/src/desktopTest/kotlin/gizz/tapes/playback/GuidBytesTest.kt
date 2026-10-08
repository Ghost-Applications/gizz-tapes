package gizz.tapes.playback

import kotlin.test.Test
import kotlin.test.assertContentEquals

class GuidBytesTest {

    @Test
    fun `first three groups are little-endian and the rest are as written`() {
        val expected = listOf(
            0xf4, 0x3f, 0xfa, 0x99, // Data1 99fa3ff4
            0x42, 0x17, // Data2 1742
            0xa6, 0x42, // Data3 42a6
            0x90, 0x2e, 0x08, 0x7d, 0x41, 0xf9, 0x65, 0xec, // Data4
        ).map { it.toByte() }.toByteArray()

        assertContentEquals(expected, guidBytes("99fa3ff4-1742-42a6-902e-087d41f965ec"))
    }
}
