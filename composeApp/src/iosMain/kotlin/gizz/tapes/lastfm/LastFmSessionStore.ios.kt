package gizz.tapes.lastfm

import gizz.tapes.AppContext
import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.alloc
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.value
import kotlinx.serialization.json.Json
import platform.CoreFoundation.CFDictionaryAddValue
import platform.CoreFoundation.CFDictionaryCreateMutable
import platform.CoreFoundation.CFDictionaryRef
import platform.CoreFoundation.CFRelease
import platform.CoreFoundation.CFStringRef
import platform.CoreFoundation.CFTypeRef
import platform.CoreFoundation.CFTypeRefVar
import platform.CoreFoundation.kCFBooleanTrue
import platform.CoreFoundation.kCFTypeDictionaryKeyCallBacks
import platform.CoreFoundation.kCFTypeDictionaryValueCallBacks
import platform.Foundation.CFBridgingRelease
import platform.Foundation.CFBridgingRetain
import platform.Foundation.NSData
import platform.Foundation.NSString
import platform.Foundation.NSUTF8StringEncoding
import platform.Foundation.create
import platform.Foundation.dataUsingEncoding
import platform.Security.SecItemAdd
import platform.Security.SecItemCopyMatching
import platform.Security.SecItemDelete
import platform.Security.errSecSuccess
import platform.Security.kSecAttrAccessible
import platform.Security.kSecAttrAccessibleAfterFirstUnlock
import platform.Security.kSecAttrAccount
import platform.Security.kSecAttrService
import platform.Security.kSecClass
import platform.Security.kSecClassGenericPassword
import platform.Security.kSecMatchLimit
import platform.Security.kSecMatchLimitOne
import platform.Security.kSecReturnData
import platform.Security.kSecValueData

actual fun createLastFmSessionStore(appContext: AppContext): LastFmSessionStore = KeychainLastFmSessionStore()

/** Session stored as a Keychain generic password. */
@OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
private class KeychainLastFmSessionStore : LastFmSessionStore {

    override fun load(): LastFmSession? = memScoped {
        val result = alloc<CFTypeRefVar>()
        val status = query(kSecReturnData to kCFBooleanTrue, kSecMatchLimit to kSecMatchLimitOne) {
            SecItemCopyMatching(it, result.ptr)
        }
        if (status != errSecSuccess) return null
        val data = CFBridgingRelease(result.value) as? NSData ?: return null
        val json = NSString.create(data, NSUTF8StringEncoding)?.toString() ?: return null
        runCatching { Json.decodeFromString<LastFmSession>(json) }.getOrNull()
    }

    override fun save(session: LastFmSession) {
        clear()
        val data = (Json.encodeToString(session) as NSString).dataUsingEncoding(NSUTF8StringEncoding) ?: return
        val value = CFBridgingRetain(data)
        try {
            query(kSecValueData to value, kSecAttrAccessible to kSecAttrAccessibleAfterFirstUnlock) {
                SecItemAdd(it, null)
            }
        } finally {
            CFRelease(value)
        }
    }

    override fun clear() {
        query { SecItemDelete(it) }
    }

    private fun <T> query(vararg extra: Pair<CFStringRef?, CFTypeRef?>, block: (CFDictionaryRef?) -> T): T {
        val service = CFBridgingRetain(SERVICE)
        val account = CFBridgingRetain(ACCOUNT)
        val items = listOf(
            kSecClass to kSecClassGenericPassword,
            kSecAttrService to service,
            kSecAttrAccount to account,
        ) + extra
        val dict = CFDictionaryCreateMutable(
            null,
            items.size.toLong(),
            kCFTypeDictionaryKeyCallBacks.ptr,
            kCFTypeDictionaryValueCallBacks.ptr
        )
        items.forEach { (k, v) -> CFDictionaryAddValue(dict, k, v) }
        try {
            return block(dict)
        } finally {
            CFRelease(dict)
            CFRelease(service)
            CFRelease(account)
        }
    }

    private companion object {
        const val SERVICE = "gizz.tapes.lastfm"
        const val ACCOUNT = "session"
    }
}
