package gizz.tapes

import coil3.PlatformContext
import okio.FileSystem
import okio.Path

internal actual fun imageCacheDirectory(context: PlatformContext): Path =
    FileSystem.SYSTEM_TEMPORARY_DIRECTORY / "gizz_tapes_image_cache"
