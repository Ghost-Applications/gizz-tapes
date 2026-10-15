package gizz.tapes.lastfm

import co.touchlab.kermit.Logger
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import gizz.tapes.Platform
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

sealed interface LastFmAuthState {
    data object LoggedOut : LastFmAuthState
    data object Authorizing : LastFmAuthState
    data class LoggedIn(val name: String) : LastFmAuthState
}

/**
 * Last.fm token auth (https://www.last.fm/api/desktopauth): get a token, have the user approve it in
 * a browser, then exchange it for a session key. Mobile passes a `gizztapes://lastfm` callback so the
 * exchange happens as soon as the browser hands back to the app; polling covers desktop and the case
 * where Last.fm doesn't follow the callback.
 */
@Inject
@SingleIn(AppScope::class)
class LastFmAuthManager(
    private val api: LastFmApiClient,
    private val store: LastFmSessionStore,
    private val platform: Platform,
) {
    private val logger = Logger.withTag("LastFmAuthManager")
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val exchangeMutex = Mutex()

    private val _session = MutableStateFlow(store.load())
    val session: StateFlow<LastFmSession?> = _session.asStateFlow()

    private val _state = MutableStateFlow(_session.value.toState())
    val state: StateFlow<LastFmAuthState> = _state.asStateFlow()

    private var pendingToken: String? = null
    private var pollJob: Job? = null

    /** Starts auth and returns the URL the user must open, or null if a token couldn't be fetched. */
    suspend fun startAuth(): String? {
        cancelAuth()
        val token = api.getToken()
            .onLeft { logger.e(it) { "Unable to get Last.fm token" } }
            .getOrNull() ?: return null

        pendingToken = token
        _state.value = LastFmAuthState.Authorizing
        pollJob = scope.launch {
            repeat((POLL_TIMEOUT / POLL_INTERVAL).toInt()) {
                delay(POLL_INTERVAL)
                if (exchange(token)) return@launch
            }
            logger.w { "Timed out waiting for Last.fm approval" }
            cancelAuth()
        }
        return api.authUrl(token, CALLBACK_URL.takeIf { platform != Platform.Desktop })
    }

    /** Called when the browser redirects back to the app after the user approves. */
    fun onCallback() {
        val token = pendingToken ?: return
        scope.launch { exchange(token) }
    }

    fun cancelAuth() {
        pollJob?.cancel()
        pollJob = null
        pendingToken = null
        if (_state.value is LastFmAuthState.Authorizing) _state.value = LastFmAuthState.LoggedOut
    }

    fun logout() {
        cancelAuth()
        scope.launch { store.clear() }
        _session.value = null
        _state.value = LastFmAuthState.LoggedOut
    }

    /** Returns true once the token has been resolved, successfully or not. */
    private suspend fun exchange(token: String): Boolean = exchangeMutex.withLock {
        if (pendingToken != token) return true
        api.getSession(token).fold(
            ifLeft = { error ->
                // Not approved yet, or a network blip: keep waiting.
                if (error !is LastFmError || error.isTokenNotAuthorized || error.isRetryable) return false
                logger.e(error) { "Last.fm token rejected" }
                cancelAuth()
                true
            },
            ifRight = { session ->
                store.save(session)
                pendingToken = null
                pollJob?.cancel()
                _session.value = session
                _state.value = LastFmAuthState.LoggedIn(session.name)
                true
            }
        )
    }

    private fun LastFmSession?.toState() = this?.let { LastFmAuthState.LoggedIn(it.name) } ?: LastFmAuthState.LoggedOut

    companion object {
        const val CALLBACK_URL = "gizztapes://lastfm"
        private val POLL_INTERVAL = 3.seconds
        private val POLL_TIMEOUT = 5.minutes
    }
}
