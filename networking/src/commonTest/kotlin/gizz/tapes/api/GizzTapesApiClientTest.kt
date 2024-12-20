package gizz.tapes.api

import gizz.tapes.api.data.PartialShowData
import gizz.tapes.api.data.Show
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class GizzTapesApiClientTest {

    @Test
    fun getShows() = runTest {
        val mockEngine = MockEngine {
            respond(
                content = showsJson,
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json")
            )
        }

        val client = GizzTapesApiClient(HttpClient(mockEngine))

        val result = client.shows()
        assertIs<List<PartialShowData>>(result.getOrNull())
    }

    @Test
    fun getShow() = runTest {
        val mockEngine = MockEngine {
            respond(
                content = showJson,
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json")
            )
        }

        val client = GizzTapesApiClient(HttpClient(mockEngine))

        val result = client.show("123")
        result.onLeft { throw it }
        assertIs<Show>(result.getOrNull())
    }

    @Test
    fun should_fail_when_default_config_is_overridden_to_not_allow_unknown_keys() = runTest {
        val mockEngine = MockEngine {
            respond(
                content = """
                [{
                    "unknown_field": "shouldn't error",
                    "id": "2024-09-08",
                    "date": "2024-09-08",
                    "venuename": "Red Rocks Amphitheatre",
                    "location": "Morrison, CO, USA",
                    "title": "",
                    "order": 1,
                    "poster_url": "https://kglw.net/i/poster-art-1699403394.jpeg"
                }]
            """.trimIndent(),
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json")
            )
        }

        val client = GizzTapesApiClient(HttpClient(mockEngine) {
            install(ContentNegotiation) {
                json()
            }
        })
        assertIs<Throwable>(client.shows().leftOrNull(), "Default http client is overriding the provided client")
    }


    @Test
    fun default_config_should_allow_extra_unknown_fields() = runTest {
        val mockEngine = MockEngine {
            respond(
                content = """
                [{
                    "unknown_field": "shouldn't error",
                    "id": "2024-09-08",
                    "date": "2024-09-08",
                    "venuename": "Red Rocks Amphitheatre",
                    "location": "Morrison, CO, USA",
                    "title": "",
                    "order": 1,
                    "poster_url": "https://kglw.net/i/poster-art-1699403394.jpeg"
                }]
            """.trimIndent(),
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json")
            )
        }

        val client = GizzTapesApiClient(HttpClient(mockEngine))
        val result = client.shows()
        assertIs<List<PartialShowData>>(result.getOrNull())
    }

    @Test
    fun in_memory_cache_should_work_by_default() = runTest {
        var networkCalls = 0
        val mockEngine = MockEngine {
            networkCalls++
            respond(
                content = """
                [{
                    "id": "2024-09-08",
                    "date": "2024-09-08",
                    "venuename": "Red Rocks Amphitheatre",
                    "location": "Morrison, CO, USA",
                    "title": "",
                    "order": 1,
                    "poster_url": "https://kglw.net/i/poster-art-1699403394.jpeg"
                }]
            """.trimIndent(),
                status = HttpStatusCode.OK,
                headers = headersOf(
                    HttpHeaders.ContentType to listOf("application/json"),
                    HttpHeaders.ETag to listOf("W/\"1045e7e51d96a322a7ae1abfda8a77e8eaad34b21e5536e1b04a940853c133b3\""),
                    HttpHeaders.CacheControl to listOf("max-age=600")
                )
            )
        }

        val client = GizzTapesApiClient(HttpClient(mockEngine))
        val result = client.shows()
        assertIs<List<PartialShowData>>(result.getOrNull())

        val result2 = client.shows()
        assertIs<List<PartialShowData>>(result2.getOrNull())

        assertEquals(1, networkCalls)
    }
}
