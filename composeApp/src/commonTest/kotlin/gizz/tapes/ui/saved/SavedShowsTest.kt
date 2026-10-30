package gizz.tapes.ui.saved

import androidx.datastore.core.DataStore
import arrow.core.Either
import arrow.core.right
import gizz.tapes.api.GizzTapesApiClient
import gizz.tapes.api.data.PartialShowData
import gizz.tapes.data.DownloadedShowData
import gizz.tapes.data.ShowId
import gizz.tapes.storage.SavedShows
import gizz.tapes.storage.SavedShowsRepository
import gizz.tapes.util.LCE
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.datetime.LocalDate
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalCoroutinesApi::class)
class SavedShowsTest {

    private val allShows = listOf(
        show("a", LocalDate(2023, 1, 1)),
        show("b", LocalDate(2024, 6, 1), order = 1u),
        show("c", LocalDate(2024, 6, 1), order = 2u),
        show("d", LocalDate(2022, 3, 3)),
    )

    private var showsRequests = 0

    private val apiClient = object : GizzTapesApiClient {
        override suspend fun shows(): Either<Exception, List<PartialShowData>> {
            showsRequests++
            return allShows.right()
        }

        override suspend fun show(id: String) = unused()
        override suspend fun search(query: String) = unused()
        override suspend fun search(query: String, showTagIds: Set<UInt>, recordingTypeIds: Set<UInt>) = unused()
        override suspend fun heroPhotos() = unused()
        override suspend fun years() = unused()
        override suspend fun stats() = unused()
        override suspend fun countries() = unused()
        override suspend fun venues() = unused()
        override suspend fun showTags() = unused()
        override suspend fun recordingTypes() = unused()
        override suspend fun artists() = unused()
        override suspend fun tapers() = unused()
    }

    private val dataStore = object : DataStore<SavedShows> {
        val state = MutableStateFlow(SavedShows())
        override val data: Flow<SavedShows> = state
        override suspend fun updateData(transform: suspend (t: SavedShows) -> SavedShows): SavedShows =
            state.value.let { transform(it) }.also { state.value = it }
    }

    private val repository = SavedShowsRepository(dataStore)

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `saved ids are joined with the show list and unknown ids are dropped`() = runTest {
        val ids = MutableStateFlow(listOf(ShowId("b"), ShowId("missing"), ShowId("a")))

        val result = savedShows(apiClient, ids) { it }.first()

        assertEquals(listOf("b", "a"), result.showIds())
    }

    @Test
    fun `changing saved ids re-emits without refetching the show list`() = runTest {
        val ids = MutableStateFlow(listOf(ShowId("a")))
        val latest = MutableStateFlow<LCE<List<DownloadedShowData>, Exception>>(LCE.Loading)
        val job = launch { savedShows(apiClient, ids) { it }.collect { latest.value = it } }

        latest.first { it.showIds() == listOf("a") }
        ids.value = listOf(ShowId("c"), ShowId("a"))
        latest.first { it.showIds() == listOf("c", "a") }
        job.cancelAndJoin()

        assertEquals(1, showsRequests)
    }

    @Test
    fun `recent shows keep play order`() = runTest {
        listOf("a", "d", "c").forEach { repository.addRecentShow(ShowId(it)) }

        val viewModel = RecentShowsViewModel(apiClient, repository)

        assertEquals(listOf("c", "d", "a"), viewModel.shows.first { it is LCE.Content }.showIds())
    }

    @Test
    fun `favorite shows are sorted newest first then by order`() = runTest {
        listOf("b", "d", "c", "a").forEach { repository.toggleFavorite(ShowId(it)) }

        val viewModel = FavoriteShowsViewModel(apiClient, repository)

        assertEquals(listOf("c", "b", "a", "d"), viewModel.shows.first { it is LCE.Content }.showIds())
    }

    @Test
    fun `unfavoriting a show removes it from favorites`() = runTest {
        listOf("a", "b").forEach { repository.toggleFavorite(ShowId(it)) }
        val viewModel = FavoriteShowsViewModel(apiClient, repository)
        viewModel.shows.first { it.showIds() == listOf("b", "a") }

        repository.toggleFavorite(ShowId("b"))

        assertEquals(listOf("a"), viewModel.shows.first { it.showIds() == listOf("a") }.showIds())
    }

    private fun LCE<List<DownloadedShowData>, Exception>.showIds(): List<String>? =
        (this as? LCE.Content)?.value?.map { it.showId.value }

    private fun show(id: String, date: LocalDate, order: UShort = 0u) = PartialShowData(
        id = id,
        date = date,
        venueName = "Venue $id",
        location = "Location $id",
        title = null,
        order = order,
        posterUrl = "https://example.com/$id.jpg",
        averageRating = null,
        countRatings = 0f,
        weightedRating = null,
        tags = emptyList(),
        artistName = "King Gizzard & The Lizard Wizard",
    )
}

private fun unused(): Nothing = error("not used by saved shows")
