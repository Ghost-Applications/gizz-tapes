package gizz.tapes.data

import androidx.datastore.core.Serializer
import gizz.tapes.api.data.Recording
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import kotlinx.serialization.json.encodeToStream
import java.io.InputStream
import java.io.OutputStream
import javax.inject.Inject
import javax.inject.Singleton

@Serializable
data class Settings(
    val preferredRecordingType: Recording.Type
)

@Singleton
class SettingsSerializer @Inject constructor(): Serializer<Settings> {
    override val defaultValue: Settings = Settings(
        preferredRecordingType = Recording.Type.SBD
    )

    @OptIn(ExperimentalSerializationApi::class)
    override suspend fun readFrom(input: InputStream): Settings {
        return Json.decodeFromStream<Settings>(input)
    }

    @OptIn(ExperimentalSerializationApi::class)
    override suspend fun writeTo(t: Settings, output: OutputStream) {
        Json.encodeToStream(t, output)
    }
}
