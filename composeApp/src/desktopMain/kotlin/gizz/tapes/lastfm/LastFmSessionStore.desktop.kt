package gizz.tapes.lastfm

import gizz.tapes.AppContext
import kotlinx.serialization.json.Json
import java.io.File
import java.nio.file.Files
import java.nio.file.attribute.PosixFilePermissions

actual fun createLastFmSessionStore(appContext: AppContext): LastFmSessionStore = FileLastFmSessionStore(appContext)

/**
 * Plain file readable only by the current user. Weaker than Keychain/Keystore, but avoids a native
 * keyring dependency per OS; the key only allows scrobbling and can be revoked from last.fm.
 */
private class FileLastFmSessionStore(appContext: AppContext) : LastFmSessionStore {
    private val file = appContext.settingsPath.parent!!.resolve("lastfm_session.json").toFile()

    override fun load(): LastFmSession? = runCatching {
        file.takeIf(File::exists)?.let { Json.decodeFromString<LastFmSession>(it.readText()) }
    }.getOrNull()

    override fun save(session: LastFmSession) {
        file.parentFile.mkdirs()
        file.writeText("")
        runCatching { Files.setPosixFilePermissions(file.toPath(), PosixFilePermissions.fromString("rw-------")) }
        file.writeText(Json.encodeToString(session))
    }

    override fun clear() {
        file.delete()
    }
}
