package gizz.tapes.storage

import androidx.datastore.core.DataStore
import gizz.tapes.data.ShowId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class SavedShowsRepositoryTest {

    private val dataStore = object : DataStore<SavedShows> {
        val state = MutableStateFlow(SavedShows())
        override val data: Flow<SavedShows> = state
        override suspend fun updateData(transform: suspend (t: SavedShows) -> SavedShows): SavedShows =
            state.value.let { transform(it) }.also { state.value = it }
    }

    private val repository = SavedShowsRepository(dataStore)

    private suspend fun play(vararg ids: String) = ids.forEach { repository.addRecentShow(ShowId(it)) }

    private suspend fun favorite(vararg ids: String) = ids.forEach { repository.toggleFavorite(ShowId(it)) }

    private suspend fun recentIds() = repository.recentShowIds.first().map { it.value }

    private suspend fun favoriteIds() = repository.favoriteShowIds.first().map { it.value }

    @Test
    fun `most recently played show is first`() = runTest {
        play("a", "b", "c")

        assertEquals(listOf("c", "b", "a"), recentIds())
    }

    @Test
    fun `replaying a show moves it to the top without duplicating it`() = runTest {
        play("a", "b", "a")

        assertEquals(listOf("a", "b"), recentIds())
    }

    @Test
    fun `toggling a show adds it to favorites`() = runTest {
        favorite("a", "b")

        assertEquals(listOf("b", "a"), favoriteIds())
    }

    @Test
    fun `toggling a favorited show removes it`() = runTest {
        favorite("a", "b", "a")

        assertEquals(listOf("b"), favoriteIds())
    }

    @Test
    fun `favorites and recents are independent`() = runTest {
        play("a")
        favorite("b")

        assertEquals(listOf("a"), recentIds())
        assertEquals(listOf("b"), favoriteIds())
    }
}
