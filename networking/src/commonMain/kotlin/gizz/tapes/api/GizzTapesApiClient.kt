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

interface GizzTapesApiClient {
    companion object {
        operator fun invoke(): GizzTapesApiClient = RealGizzTapesApiClient()
        operator fun invoke(client: HttpClient): GizzTapesApiClient = RealGizzTapesApiClient(client)
    }

    suspend fun shows(): Either<Throwable, List<PartialShowData>>
    suspend fun show(id: String): Either<Throwable, Show>
}

private class RealGizzTapesApiClient(
    client: HttpClient = HttpClient(),
) : GizzTapesApiClient {
    private val client = client.config {
        // default in memory cache, clients can override with disk cache.
        install(HttpCache)
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
            })
        }
    }

    override suspend fun shows(): Either<Throwable, List<PartialShowData>> = Either.catch {
        client.get("https://tapes.kglw.net/api/v1/shows.json").body()
    }

    override suspend fun show(id: String): Either<Throwable, Show> = Either.catch {
        client.get("https://tapes.kglw.net/api/v1/shows/$id.json").body()
    }
}
