package gizz.tapes.api

import arrow.core.Either
import gizz.tapes.api.data.PartialShowData
import gizz.tapes.api.data.Show
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.cache.HttpCache
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

class GizzTapesApiClient(
    client: HttpClient = HttpClient(),
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

    suspend fun shows(): Either<Throwable, List<PartialShowData>> = Either.catch {
        client.get("https://tapes.kglw.net/api/v1/shows.json").body()
    }

    suspend fun show(id: String): Either<Throwable, Show> = Either.catch {
        client.get("https://tapes.kglw.net/api/v1/shows/$id.json").body()
    }
}
