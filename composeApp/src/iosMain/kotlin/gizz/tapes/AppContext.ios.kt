package gizz.tapes

import okio.Path
import okio.Path.Companion.toPath
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSURL
import platform.Foundation.NSUserDomainMask

actual class AppContext {
    actual val platform: Platform = Platform.iOS
    actual val settingsPath: Path = documentsDir() / "gizz_tapes_settings.json"
    actual val sessionPath: Path = documentsDir() / "gizz_tapes_session.json"
    actual val savedShowsPath: Path = documentsDir() / "gizz_tapes_saved_shows.json"
    actual val downloadsPath: Path = documentsDir() / "downloads"
    actual val scrobbleQueuePath: Path = documentsDir() / "gizz_tapes_scrobble_queue.json"
}

private fun documentsDir(): Path {
    val urls = NSFileManager.defaultManager().URLsForDirectory(NSDocumentDirectory, NSUserDomainMask)
    val path = (urls.firstOrNull() as? NSURL)?.path
        ?: platform.Foundation.NSTemporaryDirectory()
    return path.toPath()
}
