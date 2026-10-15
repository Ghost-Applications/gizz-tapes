package gizz.tapes.lastfm

import arrow.core.Either
import io.ktor.client.HttpClient
import io.ktor.client.request.forms.submitForm
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.statement.bodyAsText
import io.ktor.http.URLBuilder
import io.ktor.http.parameters
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okio.ByteString.Companion.encodeUtf8

/** Error returned by the Last.fm API. See https://www.last.fm/api/errorcodes */
class LastFmError(val code: Int, message: String) : Exception("Last.fm error $code: $message") {
    /** Last.fm asks clients to retry later on these (11 offline, 16 temporary, 29 rate limited). */
    val isRetryable: Boolean get() = code in RETRYABLE_CODES

    /** The session key was revoked, the user needs to authenticate again. */
    val isInvalidSession: Boolean get() = code == INVALID_SESSION

    /** auth.getSession called before the user approved the token. */
    val isTokenNotAuthorized: Boolean get() = code == TOKEN_NOT_AUTHORIZED

    companion object {
        const val INVALID_SESSION = 9
        const val TOKEN_NOT_AUTHORIZED = 14
        private val RETRYABLE_CODES = setOf(11, 16, 29)
    }
}

@Serializable
data class LastFmSession(val name: String, val key: String)

@Serializable
data class LastFmTrack(
    val artist: String,
    val track: String,
    val album: String?,
    val durationSeconds: Long?,
)

@Serializable
data class LastFmScrobble(
    val track: LastFmTrack,
    /** Unix time in seconds (UTC) the track started playing. */
    val timestamp: Long,
)

data class ScrobbleResult(val accepted: Int, val ignored: Int)

interface LastFmApiClient {
    companion object {
        const val MAX_SCROBBLES_PER_REQUEST = 50

        operator fun invoke(
            apiKey: String,
            sharedSecret: String,
            client: HttpClient = HttpClient(),
        ): LastFmApiClient = RealLastFmApiClient(apiKey, sharedSecret, client)
    }

    /** URL the user opens in a browser to approve [token]. */
    fun authUrl(token: String, callback: String? = null): String

    suspend fun getToken(): Either<Exception, String>
    suspend fun getSession(token: String): Either<Exception, LastFmSession>
    suspend fun updateNowPlaying(sessionKey: String, track: LastFmTrack): Either<Exception, Unit>

    /** At most [MAX_SCROBBLES_PER_REQUEST] scrobbles per call. */
    suspend fun scrobble(sessionKey: String, scrobbles: List<LastFmScrobble>): Either<Exception, ScrobbleResult>
}

private const val API_URL = "https://ws.audioscrobbler.com/2.0/"
private const val AUTH_URL = "https://www.last.fm/api/auth/"

/** api_sig per https://www.last.fm/api/authspec section 8: sorted name+value pairs, secret appended, md5. */
internal fun lastFmSignature(params: Map<String, String>, sharedSecret: String): String {
    val joined = params.entries
        .filter { it.key != "format" && it.key != "callback" }
        .sortedBy { it.key }
        .joinToString(separator = "") { it.key + it.value }
    return (joined + sharedSecret).encodeUtf8().md5().hex()
}

internal fun scrobbleParams(scrobbles: List<LastFmScrobble>): Map<String, String> = buildMap {
    scrobbles.forEachIndexed { i, scrobble ->
        put("artist[$i]", scrobble.track.artist)
        put("track[$i]", scrobble.track.track)
        put("timestamp[$i]", scrobble.timestamp.toString())
        scrobble.track.album?.let { put("album[$i]", it) }
        scrobble.track.durationSeconds?.let { put("duration[$i]", it.toString()) }
    }
}

private class RealLastFmApiClient(
    private val apiKey: String,
    private val sharedSecret: String,
    private val client: HttpClient,
) : LastFmApiClient {
    private val json = Json { ignoreUnknownKeys = true }

    override fun authUrl(token: String, callback: String?): String = URLBuilder(AUTH_URL).apply {
        parameters.append("api_key", apiKey)
        parameters.append("token", token)
        callback?.let { parameters.append("cb", it) }
    }.buildString()

    override suspend fun getToken(): Either<Exception, String> = Either.catchOrThrow {
        get("auth.getToken").getValue("token").jsonPrimitive.content
    }

    override suspend fun getSession(token: String): Either<Exception, LastFmSession> = Either.catchOrThrow {
        val session = get("auth.getSession", mapOf("token" to token)).getValue("session").jsonObject
        LastFmSession(
            name = session.getValue("name").jsonPrimitive.content,
            key = session.getValue("key").jsonPrimitive.content,
        )
    }

    override suspend fun updateNowPlaying(sessionKey: String, track: LastFmTrack): Either<Exception, Unit> =
        Either.catchOrThrow {
            val params = buildMap {
                put("artist", track.artist)
                put("track", track.track)
                track.album?.let { put("album", it) }
                track.durationSeconds?.let { put("duration", it.toString()) }
            }
            post("track.updateNowPlaying", sessionKey, params)
        }

    override suspend fun scrobble(
        sessionKey: String,
        scrobbles: List<LastFmScrobble>,
    ): Either<Exception, ScrobbleResult> = Either.catchOrThrow {
        require(scrobbles.size <= LastFmApiClient.MAX_SCROBBLES_PER_REQUEST) { "Too many scrobbles: ${scrobbles.size}" }
        val attr = post("track.scrobble", sessionKey, scrobbleParams(scrobbles))
            .getValue("scrobbles").jsonObject
            .getValue("@attr").jsonObject
        ScrobbleResult(
            accepted = attr.getValue("accepted").jsonPrimitive.int,
            ignored = attr.getValue("ignored").jsonPrimitive.int,
        )
    }

    private suspend fun get(method: String, params: Map<String, String> = emptyMap()): JsonObject {
        val signed = signed(method, params)
        val body = client.get(API_URL) {
            signed.forEach { (k, v) -> parameter(k, v) }
        }.bodyAsText()
        return parse(body)
    }

    private suspend fun post(method: String, sessionKey: String, params: Map<String, String>): JsonObject {
        val signed = signed(method, params + ("sk" to sessionKey))
        val body = client.submitForm(
            url = API_URL,
            formParameters = parameters { signed.forEach { (k, v) -> append(k, v) } },
        ).bodyAsText()
        return parse(body)
    }

    private fun signed(method: String, params: Map<String, String>): Map<String, String> {
        val all = params + mapOf("method" to method, "api_key" to apiKey)
        return all + mapOf("api_sig" to lastFmSignature(all, sharedSecret), "format" to "json")
    }

    // Last.fm can report errors with a 200 status, so the body decides success.
    private fun parse(body: String): JsonObject {
        val obj = json.parseToJsonElement(body).jsonObject
        obj["error"]?.let { code ->
            throw LastFmError(code.jsonPrimitive.int, obj["message"]?.jsonPrimitive?.content.orEmpty())
        }
        return obj
    }
}
