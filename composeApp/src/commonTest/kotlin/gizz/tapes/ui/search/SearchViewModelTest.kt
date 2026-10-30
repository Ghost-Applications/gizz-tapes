package gizz.tapes.ui.search

import arrow.core.right
import gizz.tapes.api.GizzTapesApiClient
import gizz.tapes.api.data.PartialShowData
import gizz.tapes.api.data.RecordingType
import gizz.tapes.api.data.ShowTag
import gizz.tapes.util.LC
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalCoroutinesApi::class)
class SearchViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `recording types should be loaded into state`() = runTest {
        val classUnderTest = SearchViewModel(FakeApiClient())

        assertEquals(LC.Content(recordingTypes), latestState(classUnderTest).recordingTypes)
    }

    @Test
    fun `toggling a recording type should filter the search by it`() = runTest {
        val apiClient = FakeApiClient()
        val classUnderTest = SearchViewModel(apiClient)

        classUnderTest.toggleRecordingTypeId(3u)
        val state = latestState(classUnderTest)

        assertEquals(setOf(3u), state.selectedRecordingTypeIds)
        assertEquals(setOf(3u), apiClient.lastRecordingTypeIds)
    }

    @Test
    fun `toggling a selected recording type should remove the filter`() = runTest {
        val apiClient = FakeApiClient()
        val classUnderTest = SearchViewModel(apiClient)

        classUnderTest.toggleRecordingTypeId(3u)
        classUnderTest.toggleRecordingTypeId(3u)
        val state = latestState(classUnderTest)

        assertEquals(emptySet(), state.selectedRecordingTypeIds)
        assertEquals(emptySet(), apiClient.lastRecordingTypeIds)
    }

    private suspend fun TestScope.latestState(viewModel: SearchViewModel): SearchViewModel.State {
        val job = launch { viewModel.state.collect { } }
        advanceUntilIdle()
        val state = viewModel.state.value
        job.cancelAndJoin()
        return state
    }

    private val recordingTypes = listOf(RecordingType(1u, "AUD"), RecordingType(3u, "MTX"))

    private inner class FakeApiClient : GizzTapesApiClient {
        var lastRecordingTypeIds: Set<UInt>? = null

        override suspend fun search(query: String, showTagIds: Set<UInt>, recordingTypeIds: Set<UInt>) =
            emptyList<PartialShowData>().right().also { lastRecordingTypeIds = recordingTypeIds }
        override suspend fun showTags() = emptyList<ShowTag>().right()
        override suspend fun recordingTypes() = recordingTypes.right()
        override suspend fun tapers() = unused()
        override suspend fun shows() = unused()
        override suspend fun show(id: String) = unused()
        override suspend fun search(query: String) = unused()
        override suspend fun heroPhotos() = unused()
        override suspend fun years() = unused()
        override suspend fun stats() = unused()
        override suspend fun countries() = unused()
        override suspend fun venues() = unused()
        override suspend fun artists() = unused()

        private fun unused(): Nothing = error("not used in this test")
    }
}
