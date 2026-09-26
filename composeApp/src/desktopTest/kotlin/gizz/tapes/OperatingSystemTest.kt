package gizz.tapes

import kotlin.test.Test
import kotlin.test.assertEquals

class OperatingSystemTest {

    @Test
    fun `maps JVM os names`() {
        assertEquals(OperatingSystem.MacOs, OperatingSystem.fromName("Mac OS X"))
        assertEquals(OperatingSystem.Windows, OperatingSystem.fromName("Windows 11"))
        assertEquals(OperatingSystem.Linux, OperatingSystem.fromName("Linux"))
        assertEquals(OperatingSystem.Other("FreeBSD"), OperatingSystem.fromName("FreeBSD"))
    }
}
