package gizz.tapes.ui.settings

import androidx.datastore.core.DataStore
import arrow.core.Either
import arrow.core.left
import arrow.core.right
import gizz.tapes.Platform
import gizz.tapes.api.GizzTapesApiClient
import gizz.tapes.api.data.Recording.Type
import gizz.tapes.api.data.Taper
import gizz.tapes.data.DEFAULT_RECORDING_TYPE_RANKING
import gizz.tapes.data.Settings
import gizz.tapes.data.SettingsSerializer
import gizz.tapes.lastfm.fakeLastFmAuthManager
import gizz.tapes.util.LC
import gizz.tapes.util.LCE
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import okio.Buffer
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {

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
    fun `when datastore is loading from disc screen state should be loading`() = runTest {
        val classUnderTest = SettingsViewModel(
            dataStore = object : DataStore<Settings> {
                override val data: Flow<Settings> = flow { }
                override suspend fun updateData(transform: suspend (t: Settings) -> Settings): Settings =
                    transform(Settings())
            },
            apiClient = FakeApiClient(),
            platform = Platform.Desktop,
            lastFmAuthManager = fakeLastFmAuthManager(),
        )

        assertEquals(LC.Loading, latestState(classUnderTest))
    }

    @Test
    fun `when datastore data is loaded the value should be returned in the state`() = runTest {
        val classUnderTest = SettingsViewModel(
            dataStore = FakeDataStore(),
            apiClient = FakeApiClient(),
            platform = Platform.Desktop,
            lastFmAuthManager = fakeLastFmAuthManager(),
        )

        assertEquals(
            LC.Content(SettingsScreenState(tapers = LCE.Content(emptyList()))),
            latestState(classUnderTest)
        )
    }

    @Test
    fun `tapers should be filtered and sorted like the website`() = runTest {
        val classUnderTest = SettingsViewModel(
            dataStore = FakeDataStore(),
            apiClient = FakeApiClient(
                listOf(
                    taper(1u, "few", recordings = 2),
                    taper(2u, "puffanug", recordings = 3),
                    taper(3u, "Sam Joseph, Joe Santarpia", recordings = 30),
                    taper(4u, "anon", recordings = 10, isAnon = true),
                    taper(5u, null, recordings = 10),
                    taper(6u, "Tora Tapes", recordings = 20),
                ).right()
            ),
            platform = Platform.Desktop,
            lastFmAuthManager = fakeLastFmAuthManager(),
        )

        assertEquals(
            LCE.Content(listOf(TaperItem(6u, "Tora Tapes", 20), TaperItem(2u, "puffanug", 3))),
            (latestState(classUnderTest) as LC.Content).content.tapers
        )
    }

    @Test
    fun `taper load failure should be surfaced`() = runTest {
        val error = Exception("offline")
        val classUnderTest = SettingsViewModel(
            dataStore = FakeDataStore(),
            apiClient = FakeApiClient(error.left()),
            platform = Platform.Desktop,
            lastFmAuthManager = fakeLastFmAuthManager(),
        )

        assertEquals(LCE.Error(error), (latestState(classUnderTest) as LC.Content).content.tapers)
    }

    @Test
    fun `ranking toggle and moves should update saved ranking`() = runTest {
        val dataStore = FakeDataStore()
        val classUnderTest = SettingsViewModel(dataStore, FakeApiClient(), Platform.Desktop, fakeLastFmAuthManager())

        classUnderTest.updateRecordingTypesRanked(true)
        advanceUntilIdle()
        assertEquals(DEFAULT_RECORDING_TYPE_RANKING, dataStore.value.rankedRecordingTypes)

        classUnderTest.moveRecordingType(from = 2, to = 0)
        advanceUntilIdle()
        assertEquals(listOf(Type.AUD, Type.SBD, Type.MTX, Type.Mixed), dataStore.value.rankedRecordingTypes)

        classUnderTest.moveRecordingType(from = 0, to = -1)
        advanceUntilIdle()
        assertEquals(listOf(Type.AUD, Type.SBD, Type.MTX, Type.Mixed), dataStore.value.rankedRecordingTypes)

        classUnderTest.updateRecordingTypesRanked(false)
        advanceUntilIdle()
        assertEquals(null, dataStore.value.rankedRecordingTypes)
    }

    @Test
    fun `toggleFavouriteTaper should add then remove taper`() = runTest {
        val dataStore = FakeDataStore()
        val classUnderTest = SettingsViewModel(dataStore, FakeApiClient(), Platform.Desktop, fakeLastFmAuthManager())

        classUnderTest.toggleFavouriteTaper(2u)
        advanceUntilIdle()
        assertEquals(setOf(2u), dataStore.value.favouriteTaperIds)

        classUnderTest.toggleFavouriteTaper(2u)
        advanceUntilIdle()
        assertEquals(emptySet(), dataStore.value.favouriteTaperIds)
    }

    @Test
    fun `legacy favourite taper names should migrate to ids when tapers load`() = runTest {
        val legacy = SettingsSerializer().readFrom(Buffer().writeUtf8("""{"favouriteTapers":["puffanug"]}"""))
        val dataStore = FakeDataStore(legacy)
        val classUnderTest = SettingsViewModel(
            dataStore = dataStore,
            apiClient = FakeApiClient(
                tapers = listOf(taper(1u, "someone", recordings = 3), taper(2u, "puffanug", recordings = 3)).right()
            ),
            platform = Platform.Desktop,
            lastFmAuthManager = fakeLastFmAuthManager(),
        )

        latestState(classUnderTest)

        assertEquals(Settings(favouriteTaperIds = setOf(2u)), dataStore.value)
    }

    @Test
    fun `updateVolumeBoostEnabled should update saved volume boost setting`() = runTest {
        val dataStore = FakeDataStore()
        val classUnderTest = SettingsViewModel(dataStore, FakeApiClient(), Platform.Desktop, fakeLastFmAuthManager())

        classUnderTest.updateVolumeBoostEnabled(true)
        advanceUntilIdle()

        assertEquals(true, dataStore.value.volumeBoostEnabled)
    }

    @Test
    fun `isVolumeBoostSupported should reflect the current platform`() = runTest {
        val classUnderTest =
            SettingsViewModel(FakeDataStore(), FakeApiClient(), Platform.Android, fakeLastFmAuthManager())

        assertEquals(true, (latestState(classUnderTest) as LC.Content).content.isVolumeBoostSupported)
    }

    private suspend fun TestScope.latestState(viewModel: SettingsViewModel): LC<SettingsScreenState> {
        val job = launch { viewModel.settingsState.collect { } }
        advanceUntilIdle()
        val state = viewModel.settingsState.value
        job.cancelAndJoin()
        return state
    }

    private fun taper(id: UInt, name: String?, recordings: Int, isAnon: Boolean = false) = Taper(
        id = id,
        name = name,
        isAnon = isAnon,
        recordings = List(recordings) { Taper.TaperRecording(id = "rec$it", showId = it.toUInt()) },
    )

    private class FakeDataStore(initial: Settings = Settings()) : DataStore<Settings> {
        private val dataStateFlow = MutableStateFlow(initial)
        val value: Settings get() = dataStateFlow.value
        override val data: Flow<Settings> = dataStateFlow
        override suspend fun updateData(transform: suspend (t: Settings) -> Settings): Settings =
            transform(dataStateFlow.value).also { dataStateFlow.value = it }
    }

    private class FakeApiClient(
        private val tapers: Either<Exception, List<Taper>> = emptyList<Taper>().right(),
    ) : GizzTapesApiClient {
        override suspend fun tapers() = tapers
        override suspend fun shows() = unused()
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

        private fun unused(): Nothing = error("not used in this test")
    }
}
