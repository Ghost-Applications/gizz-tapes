package gizz.tapes.data

import androidx.datastore.core.CorruptionException
import androidx.datastore.core.okio.OkioSerializer
import dev.zacsweers.metro.Inject
import gizz.tapes.api.data.Recording
import gizz.tapes.api.data.Recording.Type.AUD
import gizz.tapes.api.data.Recording.Type.MTX
import gizz.tapes.api.data.Recording.Type.Mixed
import gizz.tapes.api.data.Recording.Type.SBD
import gizz.tapes.data.SortOrder.Ascending
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import okio.BufferedSink
import okio.BufferedSource
import okio.use

/** Order used when the user has no preference, matches tapes.kglw.net. */
val DEFAULT_RECORDING_TYPE_RANKING: List<Recording.Type> = listOf(SBD, MTX, AUD, Mixed)

@Serializable
data class Settings(
    /** Pre-ranking single preference, only read to migrate old settings files. */
    @SerialName("preferredRecordingType")
    private val legacyPreferredRecordingType: Recording.Type? = null,
    /** null means "No preference". */
    val rankedRecordingTypes: List<Recording.Type>? = null,
    val favouriteTapers: Set<String> = emptySet(),
    val yearSortOrder: SortOrder = Ascending,
    val showSortOrder: SortOrder = Ascending,
    val volumeBoostEnabled: Boolean = false,
) {
    val recordingTypeRanking: List<Recording.Type>
        get() = rankedRecordingTypes ?: DEFAULT_RECORDING_TYPE_RANKING

    internal fun migrated(): Settings {
        if (legacyPreferredRecordingType == null) return this
        return copy(
            legacyPreferredRecordingType = null,
            rankedRecordingTypes = rankedRecordingTypes
                ?: (listOf(legacyPreferredRecordingType) + DEFAULT_RECORDING_TYPE_RANKING).distinct(),
        )
    }
}

@Inject
class SettingsSerializer : OkioSerializer<Settings> {
    @OptIn(ExperimentalSerializationApi::class)
    override suspend fun readFrom(source: BufferedSource): Settings {
        try {
            return source.use {
                Json.decodeFromString<Settings>(source.readUtf8()).migrated()
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
