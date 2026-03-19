package gizz.tapes

import co.touchlab.kermit.Logger
import co.touchlab.kermit.platformLogWriter
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject

/** Debug specific initialization things. */
@Inject
@ContributesBinding(AppScope::class)
class DebugAppInitializer : AppInitializer {
    override fun invoke() {
        Logger.addLogWriter(platformLogWriter())
    }
}
