package gizz.tapes

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.DependencyGraph
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn

@SingleIn(AppScope::class)
@DependencyGraph
interface AppGraph {
    val message: String

    @Provides
    fun provideMessage(): String = "Hello, world!"
}
