package gizz.tapes

import coil3.ImageLoader
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn
import gizz.tapes.api.GizzTapesApiClient
import io.ktor.client.HttpClient
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.cache.HttpCache
import kotlin.time.Duration.Companion.seconds

interface NetworkProviders {
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
