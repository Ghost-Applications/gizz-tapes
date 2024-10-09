package gizz.tapes

import timber.log.Timber
import javax.inject.Inject

/** Release specific initialization things. */
class ReleaseAppInitializer @Inject constructor(private val crashlyticsTree: CrashlyticsTree) : AppInitializer {
    override fun invoke() {
        Timber.plant(crashlyticsTree)
    }
}
