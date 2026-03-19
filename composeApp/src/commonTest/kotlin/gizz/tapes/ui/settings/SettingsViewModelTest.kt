package gizz.tapes.ui.settings

import androidx.datastore.core.DataStore
import gizz.tapes.api.data.Recording.Type
import gizz.tapes.data.Settings
import gizz.tapes.util.LCE
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Duration.Companion.milliseconds

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
        val emittedValues = mutableListOf<LCE<SettingsScreenState, Nothing>>()
        val classUnderTest = SettingsViewModel(
            dataStore = object : DataStore<Settings> {
                override val data: Flow<Settings> = flow { }
                override suspend fun updateData(transform: suspend (t: Settings) -> Settings): Settings =
                    transform(Settings())
            }
        )

        val job = launch {
            classUnderTest.settingsState.collect { emittedValues.add(it) }
        }
        advanceUntilIdle()
        job.cancelAndJoin()
        assertEquals<List<LCE<SettingsScreenState, Nothing>>>(
            expected = listOf(LCE.Loading),
            actual = emittedValues
        )
    }

    @Test
    fun `when datastore data is loaded the value should be returned in the state`() = runTest {
        val emittedValues = mutableListOf<LCE<SettingsScreenState, Nothing>>()
        val classUnderTest = SettingsViewModel(
            dataStore = object : DataStore<Settings> {
                val dataStateFlow = MutableStateFlow(Settings())
                override val data: Flow<Settings> = dataStateFlow
                override suspend fun updateData(transform: suspend (t: Settings) -> Settings): Settings =
                    transform(dataStateFlow.value)
            }
        )

        val job = launch {
            classUnderTest.settingsState.collect { emittedValues.add(it) }
        }
        advanceUntilIdle()
        job.cancelAndJoin()
        assertEquals<List<LCE<SettingsScreenState, Nothing>>>(
            expected = listOf(LCE.Content(SettingsScreenState(Type.SBD))),
            actual = emittedValues
        )
    }

    @Test
    fun `updatePreferredRecordingType should update saved preferred recording type`() = runTest {
        val emittedValues = mutableListOf<LCE<SettingsScreenState, Nothing>>()
        val classUnderTest = SettingsViewModel(
            dataStore = object : DataStore<Settings> {
                val dataStateFlow = MutableStateFlow(Settings())
                override val data: Flow<Settings> = dataStateFlow
                override suspend fun updateData(transform: suspend (t: Settings) -> Settings): Settings {
                    val transformed = transform(dataStateFlow.value)
                    dataStateFlow.emit(transformed)
                    return transformed
                }
            }
        )

        val job = launch {
            classUnderTest.settingsState.collect { emittedValues.add(it) }
        }
        advanceTimeBy(500.milliseconds)
        classUnderTest.updatePreferredRecordingType(Type.MTX)
        advanceUntilIdle()
        job.cancelAndJoin()
        assertEquals<List<LCE<SettingsScreenState, Nothing>>>(
            expected = listOf(
                LCE.Content(SettingsScreenState(Type.SBD)),
                LCE.Content(SettingsScreenState(Type.MTX))
            ),
            actual = emittedValues
        )
    }
}
