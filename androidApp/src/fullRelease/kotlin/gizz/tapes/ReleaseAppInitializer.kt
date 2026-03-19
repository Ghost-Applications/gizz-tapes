package gizz.tapes

import co.touchlab.kermit.Logger
import com.google.firebase.crashlytics.FirebaseCrashlytics
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject

/** Release specific initialization things. */
@Inject
@ContributesBinding(AppScope::class)
class ReleaseAppInitializer : AppInitializer {
    override fun invoke() {
        Logger.addLogWriter(CrashlyticsLogWriter(FirebaseCrashlytics.getInstance()))
    }
}
