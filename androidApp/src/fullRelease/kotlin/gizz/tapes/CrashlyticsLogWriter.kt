package gizz.tapes

import co.touchlab.kermit.LogWriter
import co.touchlab.kermit.Severity
import com.google.firebase.crashlytics.FirebaseCrashlytics

private const val CRASHLYTICS_KEY_TAG = "tag"

class CrashlyticsLogWriter(private val crashlytics: FirebaseCrashlytics) : LogWriter() {
    override fun isLoggable(tag: String, severity: Severity): Boolean = severity >= Severity.Warn

    override fun log(severity: Severity, message: String, tag: String, throwable: Throwable?) {
        crashlytics.setCustomKey(CRASHLYTICS_KEY_TAG, tag)
        crashlytics.log(message)

        throwable?.let { crashlytics.recordException(it) }
    }
}
