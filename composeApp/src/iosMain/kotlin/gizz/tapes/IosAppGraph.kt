package gizz.tapes

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.DependencyGraph
import dev.zacsweers.metro.Provides

@DependencyGraph(scope = AppScope::class)
interface IosAppGraph : AppGraph {
    @DependencyGraph.Factory
    interface Factory {
        fun create(
            @Provides appContext: AppContext
        ): IosAppGraph
    }
}
