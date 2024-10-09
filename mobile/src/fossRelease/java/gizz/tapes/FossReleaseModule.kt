package gizz.tapes

import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.Interceptor
import javax.inject.Singleton

@InstallIn(SingletonComponent::class)
@Module
interface FossReleaseModule {
    @Binds abstract fun providesAppInitializer(releaseAppInitializer: FossReleaseAppInitializer): AppInitializer

    companion object {
        @Singleton
        @Provides
        fun provideEmptySetOfInterceptors(): Set<@JvmSuppressWildcards Interceptor> = emptySet()
    }
}
