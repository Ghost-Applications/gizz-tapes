package gizz.tapes.api

import gizz.tapes.api.data.Show
import gizz.tapes.api.data.ShowsData
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.cache.HttpCache
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

class GizzTapesApiClient(
    client: HttpClient = HttpClient()
) {
    private val client = client.config {
        // default in memory cache, clients can override with disk cache.
        install(HttpCache)
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
            })
        }
    }

    suspend fun getShows(): List<ShowsData> {
        return client.get("https://tapes.kglw.net/api/v1/shows.json").body()
    }

    suspend fun getShow(id: String): Show {
        return client.get("https://tapes.kglw.net/api/v1/shows/$id.json").body()
    }
}
