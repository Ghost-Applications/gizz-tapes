package gizz.tapes.storage

import androidx.datastore.core.CorruptionException
import androidx.datastore.core.DataStore
import androidx.datastore.core.okio.OkioSerializer
import dev.zacsweers.metro.Inject
import gizz.tapes.data.ShowId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import okio.BufferedSink
import okio.BufferedSource
import okio.use

@Serializable
data class SavedShows(
    // most recently played first.
    val recentShowIds: List<ShowId> = emptyList(),
    // most recently favorited first.
    val favoriteShowIds: List<ShowId> = emptyList(),
)

@Inject
class SavedShowsSerializer : OkioSerializer<SavedShows> {
    override val defaultValue: SavedShows = SavedShows()

    override suspend fun readFrom(source: BufferedSource): SavedShows {
        try {
            return source.use { Json.decodeFromString(it.readUtf8()) }
        } catch (e: SerializationException) {
            throw CorruptionException("Unable to read SavedShows", e)
        }
    }

    override suspend fun writeTo(t: SavedShows, sink: BufferedSink) {
        sink.use { it.writeUtf8(Json.encodeToString(t)) }
    }
}

@Inject
class SavedShowsRepository(
    private val dataStore: DataStore<SavedShows>,
) {

    val recentShowIds: Flow<List<ShowId>> = dataStore.data.map { it.recentShowIds }.distinctUntilChanged()

    val favoriteShowIds: Flow<List<ShowId>> = dataStore.data.map { it.favoriteShowIds }.distinctUntilChanged()

    // replaying a show moves it back to the top instead of adding a duplicate.
    suspend fun addRecentShow(showId: ShowId) {
        dataStore.updateData {
            it.copy(recentShowIds = listOf(showId) + (it.recentShowIds - showId))
        }
    }

    suspend fun toggleFavorite(showId: ShowId) {
        dataStore.updateData {
            val favorites = if (showId in it.favoriteShowIds) {
                it.favoriteShowIds - showId
            } else {
                listOf(showId) + it.favoriteShowIds
            }
            it.copy(favoriteShowIds = favorites)
        }
    }
}
