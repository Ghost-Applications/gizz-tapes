package gizz.tapes

import okio.Path
import okio.Path.Companion.toPath

actual class AppContext {
    actual val settingsPath: Path = "${System.getProperty("user.home")}/.gizztapes/settings.json".toPath()
    actual val sessionPath: Path = "${System.getProperty("user.home")}/.gizztapes/session.json".toPath()
}
