package gizz.tapes.data

import gizz.tapes.api.data.Recording.Type.AUD
import gizz.tapes.api.data.Recording.Type.MTX
import gizz.tapes.api.data.Recording.Type.Mixed
import gizz.tapes.api.data.Recording.Type.SBD
import kotlinx.coroutines.test.runTest
import okio.Buffer
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

class SettingsSerializerTest {
    private val serializer = SettingsSerializer()

    @Test
    fun `legacy preferred type should migrate to top of ranking`() = runTest {
        val settings = serializer.readFrom(Buffer().writeUtf8("""{"preferredRecordingType":"AUD"}"""))

        assertEquals(listOf(AUD, SBD, MTX, Mixed), settings.rankedRecordingTypes)
    }

    @Test
    fun `settings without a preference should have no ranking`() = runTest {
        val settings = serializer.readFrom(Buffer().writeUtf8("""{"volumeBoostEnabled":true}"""))

        assertEquals(null, settings.rankedRecordingTypes)
        assertEquals(DEFAULT_RECORDING_TYPE_RANKING, settings.recordingTypeRanking)
    }

    @Test
    fun `migrated settings should not write legacy key`() = runTest {
        val settings = serializer.readFrom(Buffer().writeUtf8("""{"preferredRecordingType":"MTX"}"""))
        val buffer = Buffer()

        serializer.writeTo(settings.copy(favouriteTapers = setOf("puffanug")), buffer)
        val written = buffer.readUtf8()

        assertFalse("preferredRecordingType" in written)
        assertEquals(
            settings.copy(favouriteTapers = setOf("puffanug")),
            serializer.readFrom(Buffer().writeUtf8(written))
        )
    }
}
