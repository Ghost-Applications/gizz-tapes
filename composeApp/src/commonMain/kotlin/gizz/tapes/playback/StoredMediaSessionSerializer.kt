package gizz.tapes.playback

import androidx.datastore.core.CorruptionException
import androidx.datastore.core.okio.OkioSerializer
import dev.zacsweers.metro.Inject
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import okio.BufferedSink
import okio.BufferedSource
import okio.use

@Inject
class StoredMediaSessionSerializer : OkioSerializer<StoredMediaSession> {
    override val defaultValue: StoredMediaSession = StoredMediaSession()

    override suspend fun readFrom(source: BufferedSource): StoredMediaSession {
        try {
            return source.use { Json.decodeFromString(it.readUtf8()) }
        } catch (e: SerializationException) {
            throw CorruptionException("Unable to read StoredMediaSession", e)
        }
    }

    override suspend fun writeTo(t: StoredMediaSession, sink: BufferedSink) {
        sink.use { it.writeUtf8(Json.encodeToString(t)) }
    }
}
