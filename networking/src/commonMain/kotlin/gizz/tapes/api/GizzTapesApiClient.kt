package gizz.tapes.api

import arrow.core.Either
import gizz.tapes.api.data.Country
import gizz.tapes.api.data.HeroPhoto
import gizz.tapes.api.data.PartialShowData
import gizz.tapes.api.data.Show
import gizz.tapes.api.data.ShowTag
import gizz.tapes.api.data.Stats
import gizz.tapes.api.data.Venue
import gizz.tapes.api.data.YearData
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.cache.HttpCache
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.client.request.parameter
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
    suspend fun search(query: String): Either<Exception, List<PartialShowData>>

    suspend fun search(
        query: String,
        showTagIds: Set<UInt>,
    ): Either<Exception, List<PartialShowData>>

    suspend fun heroPhotos(): Either<Exception, List<HeroPhoto>>
    suspend fun years(): Either<Exception, List<YearData>>
    suspend fun stats(): Either<Exception, Stats>
    suspend fun countries(): Either<Exception, List<Country>>
    suspend fun venues(): Either<Exception, List<Venue>>
    suspend fun showTags(): Either<Exception, List<ShowTag>>
}

private class RealGizzTapesApiClient(
    private val api: API,
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
        client.get("${api.url}/api/v1/shows.json").body()
    }

    override suspend fun show(id: String): Either<Exception, Show> = Either.catchOrThrow {
        client.get("${api.url}/api/v1/shows/$id.json").body()
    }

    override suspend fun search(
        query: String,
    ): Either<Exception, List<PartialShowData>> = Either.catchOrThrow {
        client.get("${api.url}/api/v1/search") {
            parameter("q", query)
        }.body()
    }

    override suspend fun search(
        query: String,
        showTagIds: Set<UInt>
    ): Either<Exception, List<PartialShowData>> = Either.catchOrThrow {
        client.get("${api.url}/api/v1/search") {
            parameter("q", query)
            showTagIds.forEach { id ->
                parameter("set_type_id", id)
            }
        }.body()
    }

    override suspend fun heroPhotos(): Either<Exception, List<HeroPhoto>> = Either.catchOrThrow {
        client.get("${api.url}/api/v1/hero_photos.json").body()
    }

    override suspend fun years(): Either<Exception, List<YearData>> = Either.catchOrThrow {
        client.get("${api.url}/api/v1/years.json").body()
    }

    override suspend fun stats(): Either<Exception, Stats> = Either.catchOrThrow {
        client.get("${api.url}/api/v1/stats.json").body()
    }

    override suspend fun countries(): Either<Exception, List<Country>> = Either.catchOrThrow {
        client.get("${api.url}/api/v1/countries.json").body()
    }

    override suspend fun venues(): Either<Exception, List<Venue>> = Either.catchOrThrow {
        client.get("${api.url}/api/v1/venues.json").body()
    }

    override suspend fun showTags(): Either<Exception, List<ShowTag>> = Either.catchOrThrow {
        client.get("${api.url}/api/v1/show_tags.json").body()
    }
}
