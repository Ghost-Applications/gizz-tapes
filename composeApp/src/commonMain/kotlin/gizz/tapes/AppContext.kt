package gizz.tapes

import okio.Path

sealed interface Platform {
    data object Android : Platform
    data object iOS : Platform
    data object Desktop : Platform
}

/** Volume boosting uses Android's LoudnessEnhancer, which has no iOS/Desktop equivalent. */
val Platform.isVolumeBoostSupported: Boolean get() = this is Platform.Android

expect class AppContext {
    val platform: Platform
    val settingsPath: Path
    val sessionPath: Path
    val downloadsPath: Path
}
