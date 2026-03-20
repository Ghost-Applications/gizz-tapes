package gizz.tapes

import android.content.Context
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.DependencyGraph
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn
import dev.zacsweers.metrox.android.MetroAppComponentProviders

@DependencyGraph(scope = AppScope::class)
interface AndroidAppGraph : AppGraph, MetroAppComponentProviders {

    val appInitializer: AppInitializer

    @DependencyGraph.Factory
    interface Factory {
        fun create(
            @SingleIn(AppScope::class)
            @Provides appContext: AppContext
        ): AndroidAppGraph
    }

    @Provides
    @SingleIn(AppScope::class)
    fun provideContext(appContext: AppContext): Context = appContext.context
}
