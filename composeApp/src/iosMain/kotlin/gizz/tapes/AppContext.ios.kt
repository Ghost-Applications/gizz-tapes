package gizz.tapes

import okio.Path
import okio.Path.Companion.toPath
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSURL
import platform.Foundation.NSUserDomainMask

actual class AppContext {
    actual val settingsPath: Path = documentsDir() / "gizz_tapes_settings.json"
    actual val sessionPath: Path = documentsDir() / "gizz_tapes_session.json"
}

private fun documentsDir(): Path {
    val urls = NSFileManager.defaultManager().URLsForDirectory(NSDocumentDirectory, NSUserDomainMask)
    val path = (urls.firstOrNull() as? NSURL)?.path
        ?: platform.Foundation.NSTemporaryDirectory()
    return path.toPath()
}
