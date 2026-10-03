package gizz.tapes.data

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import gizz.tapes.api.GizzTapesApiClient
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

@Inject
@SingleIn(AppScope::class)
class ArtistRepository(
    private val apiClient: GizzTapesApiClient,
) {
    private val mutex = Mutex()
    private var artists: Map<UInt, String>? = null

    // falls back to BAND_NAME when the lookup fails (e.g. offline) - failures aren't cached so
    // the next call retries.
    suspend fun artistName(id: UInt?): String {
        if (id == null) return BAND_NAME
        val names = mutex.withLock {
            artists ?: apiClient.artists().getOrNull()
                ?.associate { it.id to it.name }
                ?.also { artists = it }
        }
        return names?.get(id) ?: BAND_NAME
    }
}
