package gizz.tapes.playback

import androidx.datastore.core.DataStore
import co.touchlab.kermit.Logger
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.flow.first
import kotlinx.serialization.Serializable

@Serializable
data class StoredMediaSession(
    val currentTrack: Int = 0,
    val currentTime: Long = 0,
    val items: List<PlaybackItem> = emptyList()
)

@Inject
class CurrentlyPlayingSaver(
    private val dataStorage: DataStore<StoredMediaSession>,
) {
    private val logger = Logger.withTag("CurrentlyPlayingSaver")

    suspend fun storedSession(): StoredMediaSession {
        logger.d { "storedSession()" }
        return dataStorage.data.first()
    }

    suspend fun save(
        items: List<PlaybackItem>,
        currentTrackIndex: Int,
        currentPosition: Long,
    ) {
        logger.d { "save() currentTrackIndex=$currentTrackIndex currentPosition=$currentPosition" }
        dataStorage.updateData {
            StoredMediaSession(
                currentTrack = currentTrackIndex,
                currentTime = currentPosition,
                items = items
            )
        }
    }
}
