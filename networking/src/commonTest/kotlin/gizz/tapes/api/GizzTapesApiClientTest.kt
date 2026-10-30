package gizz.tapes.api

import gizz.tapes.api.data.Country
import gizz.tapes.api.data.HeroPhoto
import gizz.tapes.api.data.PartialShowData
import gizz.tapes.api.data.RecordingType
import gizz.tapes.api.data.Show
import gizz.tapes.api.data.Stats
import gizz.tapes.api.data.Taper
import gizz.tapes.api.data.Venue
import gizz.tapes.api.data.YearData
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
    fun search() = runTest {
        var capturedQuery: String? = null
        val mockEngine = MockEngine { request ->
            capturedQuery = request.url.parameters["q"]
            respond(
                content = showsJson,
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json")
            )
        }

        val client = GizzTapesApiClient(HttpClient(mockEngine))
        val result = client.search("red rocks")
        assertIs<List<PartialShowData>>(result.getOrNull())
        assertEquals("red rocks", capturedQuery)
    }

    @Test
    fun search_with_filters_sends_repeated_ids() = runTest {
        var capturedTagIds: List<String>? = null
        var capturedRecordingTypeIds: List<String>? = null
        val mockEngine = MockEngine { request ->
            capturedTagIds = request.url.parameters.getAll("show_tag_id")
            capturedRecordingTypeIds = request.url.parameters.getAll("recording_type_id")
            respond(
                content = showsJson,
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json")
            )
        }

        val client = GizzTapesApiClient(HttpClient(mockEngine))
        val result = client.search("red rocks", showTagIds = setOf(7u), recordingTypeIds = setOf(1u, 3u))
        assertIs<List<PartialShowData>>(result.getOrNull())
        assertEquals(listOf("7"), capturedTagIds)
        assertEquals(listOf("1", "3"), capturedRecordingTypeIds)
    }

    @Test
    fun getRecordingTypes() = runTest {
        val mockEngine = MockEngine {
            respond(
                content = """[{"id":1,"name":"AUD"},{"id":2,"name":"SBD"}]""",
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json")
            )
        }

        val client = GizzTapesApiClient(HttpClient(mockEngine))

        val result = client.recordingTypes()
        assertEquals(
            listOf(RecordingType(id = 1u, name = "AUD"), RecordingType(id = 2u, name = "SBD")),
            result.getOrNull()
        )
    }

    @Test
    fun getHeroPhotos() = runTest {
        val mockEngine = MockEngine {
            respond(
                content = heroPhotosJson,
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json")
            )
        }

        val client = GizzTapesApiClient(HttpClient(mockEngine))
        val result = client.heroPhotos()
        result.onLeft { throw it }
        assertIs<List<HeroPhoto>>(result.getOrNull())
    }

    @Test
    fun getYears() = runTest {
        val mockEngine = MockEngine {
            respond(
                content = yearsJson,
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json")
            )
        }

        val client = GizzTapesApiClient(HttpClient(mockEngine))
        val result = client.years()
        result.onLeft { throw it }
        assertIs<List<YearData>>(result.getOrNull())
    }

    @Test
    fun getStats() = runTest {
        val mockEngine = MockEngine {
            respond(
                content = statsJson,
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json")
            )
        }

        val client = GizzTapesApiClient(HttpClient(mockEngine))
        val result = client.stats()
        result.onLeft { throw it }
        assertIs<Stats>(result.getOrNull())
    }

    @Test
    fun getCountries() = runTest {
        val mockEngine = MockEngine {
            respond(
                content = countriesJson,
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json")
            )
        }

        val client = GizzTapesApiClient(HttpClient(mockEngine))
        val result = client.countries()
        result.onLeft { throw it }
        assertIs<List<Country>>(result.getOrNull())
    }

    @Test
    fun getVenues() = runTest {
        val mockEngine = MockEngine {
            respond(
                content = venuesJson,
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json")
            )
        }

        val client = GizzTapesApiClient(HttpClient(mockEngine))
        val result = client.venues()
        result.onLeft { throw it }
        assertIs<List<Venue>>(result.getOrNull())
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
                    "poster_url": "https://kglw.net/i/poster-art-1699403394.jpeg",
                    "average_rating": 3.7,
                    "count_ratings": 3,
                    "weighted_rating": 4.117837112143297
                }]
                """.trimIndent(),
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json")
            )
        }

        val client = GizzTapesApiClient(
            HttpClient(mockEngine) {
                install(ContentNegotiation) {
                    json()
                }
            }
        )
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
                    "poster_url": "https://kglw.net/i/poster-art-1699403394.jpeg",
                    "average_rating": 3.7,
                    "count_ratings": 3,
                    "weighted_rating": 4.117837112143297,
                    "artist_name": "King Gizzard & The Lizard Wizard",
                    "tags": []
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
    fun getTapers() = runTest {
        val mockEngine = MockEngine {
            respond(
                content = tapersJson,
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json")
            )
        }

        val client = GizzTapesApiClient(HttpClient(mockEngine))

        val result = client.tapers()
        assertEquals(
            listOf(
                Taper(
                    id = 122u,
                    name = "Revinylize",
                    isAnon = false,
                    recordings = listOf(Taper.TaperRecording(id = "kglw2024-09-01", showId = 840u))
                ),
                Taper(id = 7u, name = null, isAnon = true, recordings = emptyList()),
            ),
            result.getOrNull()
        )
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
                    "poster_url": "https://kglw.net/i/poster-art-1699403394.jpeg",
                    "average_rating": 3.7,
                    "count_ratings": 3,
                    "weighted_rating": 4.117837112143297,
                    "artist_name": "King Gizzard & The Lizard Wizard",
                    "tags": []
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
