package gizz.tapes

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.DependencyGraph
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn
import gizz.tapes.db.Database
import gizz.tapes.db.Database.Companion.Schema
import gizz.tapes.playback.GizzMediaPlayer
import java.util.Properties

@DependencyGraph(scope = AppScope::class)
interface DesktopAppGraph : AppGraph {
    @DependencyGraph.Factory
    interface Factory {
        fun create(
            @SingleIn(AppScope::class)
            @Provides appContext: AppContext
        ): DesktopAppGraph
    }

    val mediaPlayer: GizzMediaPlayer

    @Provides
    @SingleIn(AppScope::class)
    fun provideSqlDriver(): SqlDriver {
        return JdbcSqliteDriver(
            url = "jdbc:sqlite:database.db",
            schema = Schema
        )
    }
}
