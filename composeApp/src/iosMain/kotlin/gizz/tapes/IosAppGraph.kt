package gizz.tapes

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.native.NativeSqliteDriver
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.DependencyGraph
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn
import gizz.tapes.db.Database

@DependencyGraph(scope = AppScope::class)
interface IosAppGraph : AppGraph {
    @DependencyGraph.Factory
    interface Factory {
        fun create(
            @SingleIn(AppScope::class)
            @Provides appContext: AppContext
        ): IosAppGraph
    }

    @Provides
    fun provideSqlDriver(): SqlDriver {
        return NativeSqliteDriver(
            schema = Database.Schema,
            name = "database.db"
        )
    }
}
