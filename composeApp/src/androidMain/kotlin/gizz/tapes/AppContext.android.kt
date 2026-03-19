package gizz.tapes

import android.content.Context
import okio.Path
import okio.Path.Companion.toOkioPath

actual class AppContext(
    val context: Context
) {
    actual val settingsPath: Path by lazy {
        context.filesDir.resolve("gizz_tapes_settings.json").toOkioPath()
    }
    actual val sessionPath: Path by lazy {
        context.filesDir.resolve("stored_media_session.json").toOkioPath()
    }
}
