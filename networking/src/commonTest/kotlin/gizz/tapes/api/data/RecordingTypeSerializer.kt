package gizz.tapes.api.data

import gizz.tapes.api.data.Recording.Type
import gizz.tapes.api.data.Recording.Type.AUD
import gizz.tapes.api.data.Recording.Type.MTX
import gizz.tapes.api.data.Recording.Type.None
import gizz.tapes.api.data.Recording.Type.SBD
import gizz.tapes.api.data.Recording.Type.UnknownType
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals

class RecordingTypeSerializerTest {

    @Test
    fun encode() {
        Type.entries.forEach { expected ->
            val stringValue = Json.encodeToString(Type.RecordingTypeSerializer, expected)
            assertEquals(""""$expected"""", stringValue)
        }
    }

    @Test
    fun decode() {
        listOf(
            "\"SBD\"" to SBD,
            "\"Aud\"" to AUD,
            "\"mtx\"" to MTX,
            "\"asd\"" to UnknownType,
            null to None
        ).forEach { (input, expected) ->
            val result = Json.decodeFromString(Type.RecordingTypeSerializer, "$input")
            assertEquals(expected, result)
        }
    }
}