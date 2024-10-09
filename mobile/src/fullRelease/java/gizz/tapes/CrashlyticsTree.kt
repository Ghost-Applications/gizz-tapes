package gizz.tapes

import android.util.Log
import com.google.firebase.crashlytics.FirebaseCrashlytics
import timber.log.Timber
import javax.inject.Inject

private const val CRASHLYTICS_KEY_TAG = "tag"
private val SKIP_PRIORITIES = setOf(Log.VERBOSE, Log.DEBUG, Log.INFO)

class CrashlyticsTree @Inject constructor(private val crashlytics: FirebaseCrashlytics) : Timber.Tree() {
    override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
        if (priority in SKIP_PRIORITIES) return

        crashlytics.setCustomKey(CRASHLYTICS_KEY_TAG, tag.orEmpty())
        crashlytics.log(message)

        t?.let { crashlytics.recordException(it) }
    }
}
