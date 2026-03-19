package gizz.tapes

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.DependencyGraph
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn
import gizz.tapes.playback.GizzMediaPlayer

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
}
