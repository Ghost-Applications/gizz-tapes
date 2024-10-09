package gizz.tapes

import com.google.firebase.crashlytics.FirebaseCrashlytics
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet
import okhttp3.Interceptor
import javax.inject.Singleton

@InstallIn(SingletonComponent::class)
@Module
interface ReleaseModule {
    @Binds abstract fun providesAppInitializer(releaseAppInitializer: ReleaseAppInitializer): AppInitializer

    companion object {
        @Provides
        fun providesFirebaseCrashlytics(): FirebaseCrashlytics = FirebaseCrashlytics.getInstance()

        @Singleton
        @Provides
        fun provideEmptySetOfInterceptors(): Set<@JvmSuppressWildcards Interceptor> = emptySet()
    }
}
