package gizz.tapes

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn
import gizz.tapes.api.GizzTapesApiClient
import io.ktor.client.HttpClient
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.cache.HttpCache
import kotlin.time.Duration.Companion.seconds

interface NetworkProviders {

    val gizzClientApi: GizzTapesApiClient

    @Provides
    @SingleIn(AppScope::class)
    fun provideGizzApi(): GizzTapesApiClient {
        return GizzTapesApiClient(
            HttpClient {
                install(HttpTimeout) {
                    requestTimeoutMillis = 1.5.seconds.inWholeMilliseconds
                }
                install(HttpCache)
            }
        )
    }
}
