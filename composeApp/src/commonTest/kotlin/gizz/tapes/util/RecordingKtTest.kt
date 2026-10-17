@file:OptIn(ExperimentalTime::class)

package gizz.tapes.util

import arrow.core.nel
import arrow.core.nonEmptyListOf
import gizz.tapes.api.data.InternetArchive
import gizz.tapes.api.data.KglwFile
import gizz.tapes.api.data.Recording
import gizz.tapes.api.data.Recording.Type.AUD
import gizz.tapes.api.data.Recording.Type.MTX
import gizz.tapes.api.data.Recording.Type.Mixed
import gizz.tapes.api.data.Recording.Type.SBD
import gizz.tapes.api.data.Recording.Type.UnknownType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Duration
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

class RecordingKtTest {
    private val ranking = listOf(SBD, MTX, AUD)

    @Test
    fun `should return highest ranked type present`() {
        val testData = nonEmptyListOf(
            createTestRecording("test1", UnknownType),
            createTestRecording("test2", AUD),
            createTestRecording("test3", MTX),
        )

        val result = testData.tryAndGetPreferredRecording(ranking, emptySet())

        assertEquals("test3", result.id)
    }

    @Test
    fun `should return first item if no ranked type is present`() {
        val testData = nonEmptyListOf(
            createTestRecording("test1", UnknownType),
            createTestRecording("test2", Mixed),
        )

        val result = testData.tryAndGetPreferredRecording(ranking, emptySet())

        assertEquals("test1", result.id)
    }

    @Test
    fun `should prefer favourite taper within the same type`() {
        val testData = nonEmptyListOf(
            createTestRecording("test1", SBD, taper = "someone"),
            createTestRecording("test2", SBD, taper = "puffanug"),
        )

        val result = testData.tryAndGetPreferredRecording(ranking, setOf("puffanug"))

        assertEquals("test2", result.id)
    }

    @Test
    fun `type ranking should win over favourite taper`() {
        val testData = nonEmptyListOf(
            createTestRecording("test1", AUD, taper = "puffanug"),
            createTestRecording("test2", SBD),
        )

        val result = testData.tryAndGetPreferredRecording(ranking, setOf("puffanug"))

        assertEquals("test2", result.id)
    }

    private fun createTestRecording(
        id: String,
        type: Recording.Type,
        taper: String? = null,
    ): Recording {
        return Recording(
            id = id,
            uploadedAt = Instant.fromEpochMilliseconds(1000),
            type = type,
            source = null,
            lineage = null,
            taper = taper,
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
