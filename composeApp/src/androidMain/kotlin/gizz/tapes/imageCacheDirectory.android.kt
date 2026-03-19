package gizz.tapes

import coil3.PlatformContext
import okio.Path
import okio.Path.Companion.toOkioPath

internal actual fun imageCacheDirectory(context: PlatformContext): Path =
    context.cacheDir.resolve("image_cache").toOkioPath()
