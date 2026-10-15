package gizz.tapes.lastfm

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.forms.FormDataContent
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.Parameters
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class LastFmApiClientTest {

    private val track = LastFmTrack(
        artist = "King Gizzard",
        track = "Rattlesnake",
        album = "Live",
        durationSeconds = 300
    )

    private fun client(body: String, status: HttpStatusCode = HttpStatusCode.OK, onRequest: (HttpMethod, Parameters) -> Unit = { _, _ -> }) =
        LastFmApiClient(
            apiKey = "key",
            sharedSecret = "secret",
            client = HttpClient(
                MockEngine { request ->
                    val params = (request.body as? FormDataContent)?.formData ?: request.url.parameters
                    onRequest(request.method, params)
                    respond(body, status, headersOf(HttpHeaders.ContentType, "application/json"))
                }
            )
        )

    @Test
    fun `signature matches documented example`() {
        val params = mapOf("api_key" to "xxx", "method" to "auth.getSession", "token" to "yyy", "format" to "json")
        assertEquals("006b77f9f2360b8959c28c46efe54d1e", lastFmSignature(params, "mysecret"))
    }

    @Test
    fun `scrobble params are indexed`() {
        val params = scrobbleParams(
            listOf(
                LastFmScrobble(track, 100),
                LastFmScrobble(track.copy(album = null, durationSeconds = null), 200),
            )
        )
        assertEquals(
            mapOf(
                "artist[0]" to "King Gizzard",
                "track[0]" to "Rattlesnake",
                "timestamp[0]" to "100",
                "album[0]" to "Live",
                "duration[0]" to "300",
                "artist[1]" to "King Gizzard",
                "track[1]" to "Rattlesnake",
                "timestamp[1]" to "200",
            ),
            params
        )
    }

    @Test
    fun `getSession parses session`() = runTest {
        var captured: Parameters? = null
        val api = client("""{"session":{"name":"stu","key":"sk123","subscriber":0}}""") { method, params ->
            assertEquals(HttpMethod.Get, method)
            captured = params
        }

        assertEquals(LastFmSession("stu", "sk123"), api.getSession("tok").getOrNull())
        val params = captured!!
        assertEquals("auth.getSession", params["method"])
        assertEquals("tok", params["token"])
        assertEquals("json", params["format"])
        assertEquals(
            lastFmSignature(mapOf("api_key" to "key", "method" to "auth.getSession", "token" to "tok"), "secret"),
            params["api_sig"]
        )
    }

    @Test
    fun `scrobble posts signed form and parses counts`() = runTest {
        var captured: Parameters? = null
        val api = client(
            """{"scrobbles":{"scrobble":{"track":{"#text":"Rattlesnake"}},"@attr":{"ignored":0,"accepted":1}}}"""
        ) { method, params ->
            assertEquals(HttpMethod.Post, method)
            captured = params
        }

        val result = api.scrobble("sk", listOf(LastFmScrobble(track, 100)))

        assertEquals(ScrobbleResult(accepted = 1, ignored = 0), result.getOrNull())
        val params = captured!!
        assertEquals("track.scrobble", params["method"])
        assertEquals("sk", params["sk"])
        assertEquals("Rattlesnake", params["track[0]"])
    }

    @Test
    fun `error body maps to LastFmError even with 200 status`() = runTest {
        val api = client("""{"error":9,"message":"Invalid session key"}""")

        val error = api.updateNowPlaying("sk", track).leftOrNull()

        assertIs<LastFmError>(error)
        assertEquals(9, error.code)
        assertTrue(error.isInvalidSession)
    }

    @Test
    fun `error body on non 200 status maps to LastFmError`() = runTest {
        val api = client("""{"error":16,"message":"Temporary error"}""", HttpStatusCode.ServiceUnavailable)

        val error = api.scrobble("sk", listOf(LastFmScrobble(track, 100))).leftOrNull()

        assertIs<LastFmError>(error)
        assertTrue(error.isRetryable)
    }

    @Test
    fun `authUrl includes key token and callback`() {
        val url = client("{}").authUrl("tok", "gizztapes://lastfm")
        assertEquals("https://www.last.fm/api/auth/?api_key=key&token=tok&cb=gizztapes%3A%2F%2Flastfm", url)
    }
}
