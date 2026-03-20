package gizz.tapes.ui.player

import kotlin.test.Test
import kotlin.test.assertEquals

class MediaDurationInfoTest {

    @Test
    fun `currentPositionFloat is zero when duration is zero`() {
        val info = MediaDurationInfo(currentPosition = 5000L, duration = 0L)
        assertEquals(0f, info.currentPositionFloat)
    }

    @Test
    fun `currentPositionFloat is correct ratio`() {
        val info = MediaDurationInfo(currentPosition = 1000L, duration = 4000L)
        assertEquals(0.25f, info.currentPositionFloat)
    }

    @Test
    fun `currentPositionFloat is 1 when at end`() {
        val info = MediaDurationInfo(currentPosition = 4000L, duration = 4000L)
        assertEquals(1f, info.currentPositionFloat)
    }

    @Test
    fun `elapsedTimeString formats minutes and seconds`() {
        val info = MediaDurationInfo(currentPosition = 90_000L, duration = 0L)
        assertEquals("1:30", info.elapsedTimeString)
    }

    @Test
    fun `elapsedTimeString pads single digit seconds`() {
        val info = MediaDurationInfo(currentPosition = 65_000L, duration = 0L)
        assertEquals("1:05", info.elapsedTimeString)
    }

    @Test
    fun `elapsedTimeString is zero when position is zero`() {
        val info = MediaDurationInfo(currentPosition = 0L, duration = 0L)
        assertEquals("0:00", info.elapsedTimeString)
    }

    @Test
    fun `elapsedTimeString clamps negative position to zero`() {
        val info = MediaDurationInfo(currentPosition = -1000L, duration = 0L)
        assertEquals("0:00", info.elapsedTimeString)
    }

    @Test
    fun `durationTimeString formats correctly`() {
        val info = MediaDurationInfo(currentPosition = 0L, duration = 3_723_000L)
        assertEquals("62:03", info.durationTimeString)
    }

    @Test
    fun `Empty has zero position and duration`() {
        assertEquals(0L, MediaDurationInfo.Empty.currentPosition)
        assertEquals(0L, MediaDurationInfo.Empty.duration)
    }
}
