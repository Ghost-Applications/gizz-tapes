package gizz.tapes.data

import androidx.datastore.core.Serializer
import gizz.tapes.api.data.Recording
import gizz.tapes.api.data.Recording.Type.SBD
import gizz.tapes.data.SortOrder.Ascending
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import kotlinx.serialization.json.encodeToStream
import timber.log.Timber
import java.io.InputStream
import java.io.OutputStream
import javax.inject.Inject
import javax.inject.Singleton

@Serializable
data class Settings(
    val preferredRecordingType: Recording.Type = SBD,
    val yearSortOrder: SortOrder = Ascending,
    val showSortOrder: SortOrder = Ascending,
)

@Singleton
class SettingsSerializer @Inject constructor(): Serializer<Settings> {
    override val defaultValue: Settings = Settings()

    @OptIn(ExperimentalSerializationApi::class)
    override suspend fun readFrom(input: InputStream): Settings {
        return try {
            Json.decodeFromStream<Settings>(input)
        } catch (e: Exception) {
            Timber.e(e, "Error reading settings from disc")
            defaultValue
        }
    }

    @OptIn(ExperimentalSerializationApi::class)
    override suspend fun writeTo(t: Settings, output: OutputStream) {
        try {
            Json.encodeToStream(t, output)
        } catch (e: Exception) {
            Timber.e("Error writing settings to disc")
        }
    }
}
