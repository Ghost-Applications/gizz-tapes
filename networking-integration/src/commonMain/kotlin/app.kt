import arrow.fx.coroutines.parMap
import gizz.tapes.api.API
import gizz.tapes.api.GizzTapesApiClient
import io.ktor.client.HttpClient
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logging
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.joinAll
import kotlinx.serialization.json.Json

fun main() = runBlocking {
    val api = GizzTapesApiClient(
        api = API.STAGING,
        client = HttpClient {
            install(ContentNegotiation) {
                json(
                    Json {
                        ignoreUnknownKeys = false
                    }
                )
            }
            install(Logging) {
                level = LogLevel.NONE
            }
        }
    )

    val deffered = buildList<Job> {
        async {
            api.shows().onRight { shows ->
                shows.parMap { show ->
                    api.show(show.id).onLeft {
                        throw it
                    }
                }
            }.onLeft {
                throw it
            }
        }.let { add(it) }

        async {
            api.search("red rocks").onLeft {
                throw it
            }
        }.let { add(it) }

        async {
            api.heroPhotos().onLeft {
                throw it
            }
        }.let { add(it) }

        async {
            api.years().onLeft {
                throw it
            }
        }.let { add(it) }

        async {
            api.stats().onLeft {
                throw it
            }
        }.let { add(it) }

        async {
            api.venues().onLeft {
                throw it
            }
        }.let { add(it) }

        async {
            api.countries().onLeft {
                throw it
            }
        }.let { add(it) }

        async {
            api.setTypes().onLeft {
                throw it
            }
        }.let { add(it) }
    }

    deffered.joinAll()
}
