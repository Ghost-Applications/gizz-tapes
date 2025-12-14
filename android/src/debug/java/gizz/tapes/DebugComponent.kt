package gizz.tapes

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet
import okhttp3.Interceptor
import okhttp3.logging.HttpLoggingInterceptor
import timber.log.Timber

@InstallIn(SingletonComponent::class)
@Module
class DebugComponent {

    @Provides
    @IntoSet
    fun provideHttpLoggingInterceptor(): Interceptor {
        return HttpLoggingInterceptor { message ->
            Timber.tag("OkHttpClient")
            Timber.v(message)
        }.apply { level = HttpLoggingInterceptor.Level.BASIC }
    }
}

