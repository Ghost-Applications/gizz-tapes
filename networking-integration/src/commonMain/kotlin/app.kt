import arrow.fx.coroutines.parMap
import gizz.tapes.api.GizzTapesApiClient
import io.ktor.client.HttpClient
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

fun main() = runBlocking {

    val api = GizzTapesApiClient(
        client = HttpClient {
            install(ContentNegotiation) {
                json(Json {
                    ignoreUnknownKeys = false
                })
            }
        }
    )

    val shows = api.getShows()

    shows.parMap { show ->
        println()
        val showContent = api.getShow(show.id)
        println(showContent)
        println()
    }
}