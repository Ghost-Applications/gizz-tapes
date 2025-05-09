package gizz.tapes.data

import androidx.datastore.core.okio.OkioSerializer
import gizz.tapes.api.data.Recording
import gizz.tapes.api.data.Recording.Type.SBD
import gizz.tapes.data.SortOrder.Ascending
import kotlinx.io.Buffer
import kotlinx.io.buffered
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.io.decodeFromSource
import okio.BufferedSink
import okio.BufferedSource

@Serializable
data class Settings(
    val preferredRecordingType: Recording.Type = SBD,
    val yearSortOrder: SortOrder = Ascending,
    val showSortOrder: SortOrder = Ascending,
)

class SettingsSerializer : OkioSerializer<Settings> {
    @OptIn(ExperimentalSerializationApi::class)
    override suspend fun readFrom(source: BufferedSource): Settings {
        TODO("Not yet implemented")
    }

    override suspend fun writeTo(t: Settings, sink: BufferedSink) {
        TODO("Not yet implemented")
    }

    override val defaultValue: Settings = Settings()
}

