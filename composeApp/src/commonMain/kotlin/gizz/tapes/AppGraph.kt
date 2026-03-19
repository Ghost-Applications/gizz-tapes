package gizz.tapes

import androidx.datastore.core.DataStore
import androidx.datastore.core.DataStoreFactory
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.core.okio.OkioStorage
import co.touchlab.kermit.Logger
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn
import dev.zacsweers.metrox.viewmodel.ViewModelGraph
import gizz.tapes.api.GizzTapesApiClient
import gizz.tapes.api.GizzTapesApiClient.Companion.invoke
import gizz.tapes.data.ApiErrorMessage
import gizz.tapes.data.Settings
import gizz.tapes.data.SettingsSerializer
import gizz.tapes.playback.CurrentlyPlayingSaver
import gizz.tapes.playback.StoredMediaSession
import gizz.tapes.playback.StoredMediaSessionSerializer
import io.ktor.client.HttpClient
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.cache.HttpCache
import okio.FileSystem
import okio.SYSTEM
import kotlin.time.Duration.Companion.seconds

interface AppGraph : ViewModelGraph {
    @Provides
    @SingleIn(AppScope::class)
    fun provideSettingsDataStore(
        appContext: AppContext,
        settingsSerializer: SettingsSerializer
    ): DataStore<Settings> {
        return DataStoreFactory.create(
            storage = OkioStorage(FileSystem.SYSTEM, settingsSerializer) { appContext.settingsPath },
            corruptionHandler = ReplaceFileCorruptionHandler {
                Logger.e(it) { "Corruption when reading settings" }
                Settings()
            }
        )
    }

    @Provides
    @SingleIn(AppScope::class)
    fun provideSessionDataStore(
        appContext: AppContext,
        serializer: StoredMediaSessionSerializer
    ): DataStore<StoredMediaSession> {
        FileSystem.SYSTEM.createDirectories(appContext.sessionPath.parent!!)
        return DataStoreFactory.create(
            storage = OkioStorage(FileSystem.SYSTEM, serializer) { appContext.sessionPath },
            corruptionHandler = ReplaceFileCorruptionHandler {
                Logger.e(it) { "Corruption when reading settings" }
                StoredMediaSession()
            }
        )
    }

    @Provides
    @SingleIn(AppScope::class)
    fun provideCurrentlyPlayingSaver(dataStore: DataStore<StoredMediaSession>): CurrentlyPlayingSaver =
        CurrentlyPlayingSaver(dataStore)

    @Provides
    @SingleIn(AppScope::class)
    fun provideApiErrorMessage(): ApiErrorMessage =
        ApiErrorMessage("Unable to load data. Please check your connection.")

    @Provides
    @SingleIn(AppScope::class)
    fun provideGizzApi(): GizzTapesApiClient {
        return GizzTapesApiClient(
            HttpClient {
                install(HttpTimeout) {
                    requestTimeoutMillis = 1.5.seconds.inWholeMilliseconds
                }
                install(HttpCache)
            }
        )
    }
}
