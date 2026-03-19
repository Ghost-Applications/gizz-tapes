package gizz.tapes

import coil3.PlatformContext
import okio.Path

internal expect fun imageCacheDirectory(context: PlatformContext): Path
