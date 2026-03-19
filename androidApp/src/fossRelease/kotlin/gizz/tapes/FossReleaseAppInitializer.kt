package gizz.tapes

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject

/** Foss Release specific initialization things. */
@Inject
@ContributesBinding(AppScope::class)
class FossReleaseAppInitializer : AppInitializer {
    override fun invoke() {}
}
