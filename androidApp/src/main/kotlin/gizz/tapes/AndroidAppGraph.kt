package gizz.tapes

import android.content.Context
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.DependencyGraph
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn
import dev.zacsweers.metrox.android.MetroAppComponentProviders
import gizz.tapes.db.Database
import gizz.tapes.storage.DownloadHttpClient
import gizz.tapes.storage.DownloadWorkerFactory
import io.ktor.client.HttpClient
import io.ktor.client.plugins.HttpTimeout

@DependencyGraph(scope = AppScope::class)
interface AndroidAppGraph : AppGraph, MetroAppComponentProviders {

    val appInitializer: AppInitializer
    val downloadWorkerFactory: DownloadWorkerFactory

    @DependencyGraph.Factory
    interface Factory {
        fun create(
            @Provides appContext: AppContext
        ): AndroidAppGraph
    }

    @Provides
    @SingleIn(AppScope::class)
    fun provideContext(appContext: AppContext): Context = appContext.context

    @Provides
    @SingleIn(AppScope::class)
    fun provideSqlDriver(appContext: AppContext): SqlDriver {
        return AndroidSqliteDriver(
            schema = Database.Schema,
            context = appContext.context,
            name = "database.db"
        )
    }

    @Provides
    @SingleIn(AppScope::class)
    fun provideDownloadHttpClient(): DownloadHttpClient {
        // standalone client, not derived from AppGraph's provideHttpClient(): HttpCache buffers
        // the whole response into memory to evaluate cacheability, which OOMs on large audio
        // files, and downloads are streamed straight to disk so caching buys nothing anyway.
        return DownloadHttpClient(
            HttpClient {
                install(HttpTimeout) {
                    requestTimeoutMillis = null
                }
            }
        )
    }
}
