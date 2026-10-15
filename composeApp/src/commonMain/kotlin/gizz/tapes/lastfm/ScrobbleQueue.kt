package gizz.tapes.lastfm

import androidx.datastore.core.CorruptionException
import androidx.datastore.core.okio.OkioSerializer
import dev.zacsweers.metro.Inject
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import okio.BufferedSink
import okio.BufferedSource
import okio.use

/** Scrobbles waiting to be sent, oldest first. */
@Serializable
data class ScrobbleQueue(val items: List<LastFmScrobble> = emptyList())

@Inject
class ScrobbleQueueSerializer : OkioSerializer<ScrobbleQueue> {
    override suspend fun readFrom(source: BufferedSource): ScrobbleQueue {
        try {
            return source.use { Json.decodeFromString(source.readUtf8()) }
        } catch (e: SerializationException) {
            throw CorruptionException("Unable to read ScrobbleQueue", e)
        }
    }

    override suspend fun writeTo(t: ScrobbleQueue, sink: BufferedSink) {
        sink.use { sink.writeUtf8(Json.encodeToString(t)) }
    }

    override val defaultValue: ScrobbleQueue = ScrobbleQueue()
}
