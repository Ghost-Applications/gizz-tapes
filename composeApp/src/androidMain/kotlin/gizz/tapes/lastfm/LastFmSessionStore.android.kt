package gizz.tapes.lastfm

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import gizz.tapes.AppContext
import kotlinx.serialization.json.Json
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

actual fun createLastFmSessionStore(appContext: AppContext): LastFmSessionStore = KeystoreLastFmSessionStore(appContext)

/** Session encrypted with an AES-GCM key that never leaves the Android Keystore. */
private class KeystoreLastFmSessionStore(appContext: AppContext) : LastFmSessionStore {
    private val file = appContext.context.filesDir.resolve("lastfm_session.bin")

    override fun load(): LastFmSession? = runCatching {
        if (!file.exists()) return null
        val bytes = file.readBytes()
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.DECRYPT_MODE, key(), GCMParameterSpec(TAG_BITS, bytes, 0, IV_SIZE))
        val json = cipher.doFinal(bytes, IV_SIZE, bytes.size - IV_SIZE).decodeToString()
        Json.decodeFromString<LastFmSession>(json)
    }.getOrNull()

    override fun save(session: LastFmSession) {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, key())
        val encrypted = cipher.doFinal(Json.encodeToString(session).encodeToByteArray())
        file.writeBytes(cipher.iv + encrypted)
    }

    override fun clear() {
        file.delete()
    }

    private fun key(): SecretKey {
        val keyStore = KeyStore.getInstance(KEYSTORE).apply { load(null) }
        (keyStore.getKey(KEY_ALIAS, null) as? SecretKey)?.let { return it }
        return KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, KEYSTORE).run {
            init(
                KeyGenParameterSpec.Builder(KEY_ALIAS, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                    .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                    .build()
            )
            generateKey()
        }
    }

    private companion object {
        const val KEYSTORE = "AndroidKeyStore"
        const val KEY_ALIAS = "gizz_tapes_lastfm"
        const val TRANSFORMATION = "AES/GCM/NoPadding"
        const val IV_SIZE = 12
        const val TAG_BITS = 128
    }
}
