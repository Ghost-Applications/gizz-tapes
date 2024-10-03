package gizz.tapes.api.serializer

import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Duration.Companion.seconds

class DurationInSecondsSerializerTest {

    @Test
    fun decode() {
        assertEquals(
            expected = 35.5.seconds,
            actual = Json.decodeFromString(DurationInSecondsSerializer, "35.5")
        )
    }

    @Test
    fun encode() {
        assertEquals(
            expected = "35.5",
            actual = Json.encodeToString(DurationInSecondsSerializer, 35.5.seconds),
        )

    }
}