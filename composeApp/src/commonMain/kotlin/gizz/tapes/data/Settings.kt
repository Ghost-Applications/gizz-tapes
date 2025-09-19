package gizz.tapes.data

import androidx.datastore.core.okio.OkioSerializer
import gizz.tapes.api.data.Recording
import gizz.tapes.api.data.Recording.Type.SBD
import gizz.tapes.data.SortOrder.Ascending
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okio.BufferedSink
import okio.BufferedSource
import okio.use

@Serializable
data class Settings(
    val preferredRecordingType: Recording.Type = SBD,
    val yearSortOrder: SortOrder = Ascending,
    val showSortOrder: SortOrder = Ascending,
)

class SettingsSerializer : OkioSerializer<Settings> {
    @OptIn(ExperimentalSerializationApi::class)
    override suspend fun readFrom(source: BufferedSource): Settings {
        return source.use {
            Json.decodeFromString(source.readUtf8())
        }
    }

    override suspend fun writeTo(t: Settings, sink: BufferedSink) {
        sink.use {
            val s = Json.encodeToString(t)
            sink.writeUtf8(s)
        }
    }

    override val defaultValue: Settings = Settings()
}

