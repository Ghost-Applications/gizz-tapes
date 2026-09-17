package gizz.tapes.playback

import cocoapods.google_cast_sdk.GCKCastContext
import cocoapods.google_cast_sdk.GCKCastOptions
import cocoapods.google_cast_sdk.GCKCastStateNoDevicesAvailable
import cocoapods.google_cast_sdk.GCKDiscoveryCriteria
import cocoapods.google_cast_sdk.GCKSessionManager
import cocoapods.google_cast_sdk.kGCKCastStateDidChangeNotification
import cocoapods.google_cast_sdk.kGCKDefaultMediaReceiverApplicationID
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import platform.Foundation.NSNotificationCenter
import platform.Foundation.NSOperationQueue

// Mirrors androidApp's CastOptionsProvider.kt: targets Google's stock Default Media Receiver (no
// custom receiver app), so a single Cast session works interchangeably whether it was started
// from the Android or iOS app.
//
// Lives outside Metro's DI graph on purpose, same as BackgroundDownloadSession: GCKCastContext
// must be initialized as early as possible at app launch - before any GCKUICastButton or
// IosMediaPlayer (which is only constructed on first inject, possibly after UI has already
// started composing) touches it - so it's bootstrapped directly from AppDelegate rather than
// waiting for Metro's IosAppGraph to exist.
@OptIn(ExperimentalForeignApi::class)
object GizzCastContext {
    private var initialized = false

    private val _isCastAvailable = MutableStateFlow(false)

    // Whether any Cast-enabled device is currently discoverable on the network - GCKUICastButton
    // doesn't hide itself when there's nothing to cast to, so CastButton observes this to decide
    // whether to render at all.
    val isCastAvailable: StateFlow<Boolean> = _isCastAvailable.asStateFlow()

    fun initialize() {
        if (initialized) return
        initialized = true
        val criteria = GCKDiscoveryCriteria(applicationID = kGCKDefaultMediaReceiverApplicationID)
        val options = GCKCastOptions(discoveryCriteria = criteria)
        GCKCastContext.setSharedInstanceWithOptions(options)

        updateAvailability()
        NSNotificationCenter.defaultCenter.addObserverForName(
            name = kGCKCastStateDidChangeNotification,
            `object` = null,
            queue = NSOperationQueue.mainQueue,
            usingBlock = { updateAvailability() },
        )
    }

    private fun updateAvailability() {
        _isCastAvailable.value = GCKCastContext.sharedInstance().castState() != GCKCastStateNoDevicesAvailable
    }

    val sessionManager: GCKSessionManager
        get() = GCKCastContext.sharedInstance().sessionManager()
}
