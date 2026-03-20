package gizz.tapes

import okio.Path

expect class AppContext {
    val settingsPath: Path
    val sessionPath: Path
}
