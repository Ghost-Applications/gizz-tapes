package gizz.tapes

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.DependencyGraph
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn
import gizz.tapes.db.Database.Companion.Schema
import gizz.tapes.playback.GizzMediaPlayer
import gizz.tapes.storage.DownloadHttpClient
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.HttpTimeout
import okhttp3.Dispatcher
import okio.FileSystem
import okio.Path.Companion.toPath

@DependencyGraph(scope = AppScope::class)
interface DesktopAppGraph : AppGraph {
    @DependencyGraph.Factory
    interface Factory {
        fun create(
            @Provides appContext: AppContext
        ): DesktopAppGraph
    }

    val mediaPlayer: GizzMediaPlayer

    // uses an absolute path under ~/.gizztapes (matching AppContext.desktop.kt's
    // settings/session/downloads paths) rather than a bare relative filename, since a relative
    // "database.db" resolves against whatever the JVM's working directory happens to be at
    // launch - unpredictable for a packaged app.
    @Provides
    @SingleIn(AppScope::class)
    fun provideSqlDriver(): SqlDriver {
        val databasePath = "${System.getProperty("user.home")}/.gizztapes/database.db".toPath()
        FileSystem.SYSTEM.createDirectories(databasePath.parent!!)
        return JdbcSqliteDriver(
            url = "jdbc:sqlite:$databasePath",
            schema = Schema
        )
    }

    // recording downloads all hit the same host, and OkHttp's default Dispatcher caps concurrent
    // requests per host at 5 - raising it lets more of a multi-track recording's files download
    // in parallel instead of queueing behind that limit.
    @Provides
    @SingleIn(AppScope::class)
    fun provideDownloadHttpClient(): DownloadHttpClient {
        return DownloadHttpClient(
            HttpClient(OkHttp) {
                install(HttpTimeout) {
                    requestTimeoutMillis = null
                }
                engine {
                    config {
                        dispatcher(Dispatcher().apply { maxRequestsPerHost = 10 })
                    }
                }
            }
        )
    }
}
