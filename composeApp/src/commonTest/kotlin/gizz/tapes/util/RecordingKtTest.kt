@file:OptIn(ExperimentalTime::class)

package gizz.tapes.util

import arrow.core.nel
import arrow.core.nonEmptyListOf
import gizz.tapes.api.data.InternetArchive
import gizz.tapes.api.data.KglwFile
import gizz.tapes.api.data.Recording
import gizz.tapes.api.data.Recording.Type.MTX
import gizz.tapes.api.data.Recording.Type.SBD
import gizz.tapes.api.data.Recording.Type.UnknownType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Duration
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

class RecordingKtTest {
    @Test
    fun `should return SBD when reqeuested and SBD is present`() {
        val testData = nonEmptyListOf(
            createTestRecording("test1", UnknownType),
            createTestRecording("test2", SBD),
            createTestRecording("test3", MTX),
        )

        val result = testData.tryAndGetPreferredRecordingType(SBD)

        assertEquals(SBD, result.type)
    }

    @Test
    fun `should return first item if requested is not present`() {
        val testData = nonEmptyListOf(
            createTestRecording("test1", UnknownType),
            createTestRecording("test2", MTX),
            createTestRecording("test3", MTX),
        )

        val result = testData.tryAndGetPreferredRecordingType(SBD)

        assertEquals("test1", result.id)
    }

    private fun createTestRecording(
        id: String,
        type: Recording.Type
    ): Recording {
        return Recording(
            id = id,
            uploadedAt = Instant.fromEpochMilliseconds(1000),
            type = type,
            source = null,
            lineage = null,
            taper = null,
            files =
            KglwFile(
                filename = "filename",
                length = Duration.ZERO,
                title = "title"
            ).nel(),
            filesPathPrefix = "filesPrefix",
            internetArchive = InternetArchive(
                isLma = true
            )
        )
    }
}
