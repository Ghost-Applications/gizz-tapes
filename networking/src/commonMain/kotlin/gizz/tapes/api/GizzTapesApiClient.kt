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

enum class API(val url: String) {
    PRODUCTION("https://tapes.kglw.net"),
    STAGING("https://gizztapes2-staging.fly.dev"),
}

interface GizzTapesApiClient {
    companion object {
        operator fun invoke(api: API = API.PRODUCTION): GizzTapesApiClient {
            return RealGizzTapesApiClient(api)
        }

        operator fun invoke(client: HttpClient, api: API = API.PRODUCTION): GizzTapesApiClient {
            return RealGizzTapesApiClient(api, client)
        }
    }

    suspend fun shows(): Either<Exception, List<PartialShowData>>
    suspend fun show(id: String): Either<Exception, Show>
}

private class RealGizzTapesApiClient(
    api: API,
    client: HttpClient = HttpClient(),
) : GizzTapesApiClient {
    private val client = client.config {
        // default in memory cache, clients can override with disk cache.
        install(HttpCache)
        install(ContentNegotiation) {
            json(
                Json {
                    ignoreUnknownKeys = true
                }
            )
        }
    }

    override suspend fun shows(): Either<Exception, List<PartialShowData>> = Either.catchOrThrow {
        client.get("https://tapes.kglw.net/api/v1/shows.json").body()
    }

    override suspend fun show(id: String): Either<Exception, Show> = Either.catchOrThrow {
        client.get("https://tapes.kglw.net/api/v1/shows/$id.json").body()
    }
}
