package gizz.tapes.di

import android.content.Context
import coil.ImageLoader
import coil.decode.SvgDecoder
import com.jakewharton.byteunits.DecimalByteUnit.MEGABYTES
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import gizz.tapes.R
import gizz.tapes.api.GizzTapesApiClient
import gizz.tapes.playback.MediaPlayerContainer
import gizz.tapes.playback.RealMediaPlayerContainer
import gizz.tapes.data.ApiErrorMessage
import gizz.tapes.data.PlayerErrorMessage
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.cache.HttpCache
import okhttp3.Cache
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import java.io.File
import javax.inject.Singleton
import kotlin.time.Duration.Companion.seconds

@Module
@InstallIn(SingletonComponent::class)
interface GizzTapesModule {
    companion object {
        private val DISK_CACHE_SIZE = MEGABYTES.toBytes(512)

        @Provides
        @Singleton
        fun provideGizzApi(
            okHttpClient: OkHttpClient
        ): GizzTapesApiClient {
            return GizzTapesApiClient(
                HttpClient(OkHttp) {
                    engine {
                        preconfigured = okHttpClient
                    }
                    install(HttpTimeout) {
                        requestTimeoutMillis = 1.5.seconds.inWholeMilliseconds
                    }
                    install(HttpCache)
                }
            )
        }

        @Provides
        @Singleton
        fun provideImageLoader(
            @ApplicationContext context: Context,
            okHttpClient: OkHttpClient
        ) = ImageLoader.Builder(context)
            .okHttpClient(okHttpClient)
            .components { add(SvgDecoder.Factory()) }
            .crossfade(true)
            .build()

        @Provides
        @Singleton
        fun provideCacheFile(@ApplicationContext context: Context): File = File(context.cacheDir, "http")

        @Provides
        @Singleton
        fun provideCache(
            cacheFile: File
        ): Cache = Cache(cacheFile, DISK_CACHE_SIZE)

        @Provides
        @Singleton
        fun provideApiErrorMessage(
            @ApplicationContext context: Context
        ): ApiErrorMessage = ApiErrorMessage(
            context.getString(R.string.api_error_message)
        )

        @Provides
        @Singleton
        fun providePlayerErrorMessage(
            @ApplicationContext context: Context
        ): PlayerErrorMessage = PlayerErrorMessage(
            context.getString(R.string.player_error)
        )

        @Provides
        @Singleton
        fun provideOkHttpClient(
            cache: Cache,
            interceptors: Set<@JvmSuppressWildcards Interceptor>
        ): OkHttpClient = OkHttpClient.Builder()
            .apply { interceptors.forEach { addInterceptor(it) } }
            .cache(cache)
            .build()
    }

    @Binds
    fun bindsMediaControllerContainer(container: RealMediaPlayerContainer): MediaPlayerContainer
}
