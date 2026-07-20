package gizz.tapes

import okio.Path

sealed interface Platform {
    data object Android: Platform
    data object iOS: Platform
    data object Desktop: Platform
}

expect class AppContext {
    val platform: Platform
    val settingsPath: Path
    val sessionPath: Path
}
