package gizz.tapes.data

import androidx.datastore.core.CorruptionException
import androidx.datastore.core.okio.OkioSerializer
import dev.zacsweers.metro.Inject
import gizz.tapes.api.data.Recording
import gizz.tapes.api.data.Recording.Type.SBD
import gizz.tapes.data.SortOrder.Ascending
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
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

@Inject
class SettingsSerializer : OkioSerializer<Settings> {
    @OptIn(ExperimentalSerializationApi::class)
    override suspend fun readFrom(source: BufferedSource): Settings {
        try {
            return source.use {
                Json.decodeFromString(source.readUtf8())
            }
        } catch (e: SerializationException) {
            throw CorruptionException("Unable to read Settings", e)
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
