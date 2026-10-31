package by.mazokaleh.skarnik.core.network

import android.content.Context
import by.mazokaleh.skarnik.core.config.RemoteConfig
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import java.io.File
import java.util.concurrent.TimeUnit
import javax.inject.Singleton
import kotlinx.serialization.json.Json
import okhttp3.Cache
import okhttp3.OkHttpClient

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {
    private const val CACHE_DIR = "http"
    private const val CACHE_SIZE_BYTES = 20L * 1024 * 1024
    private const val TIMEOUT_SECONDS = 20L

    /** One client for the whole app. The disk cache is created lazily by OkHttp, off the main thread. */
    @Provides
    @Singleton
    fun provideOkHttpClient(@ApplicationContext context: Context, remoteConfig: RemoteConfig): OkHttpClient =
        OkHttpClient.Builder()
            .cache(Cache(File(context.cacheDir, CACHE_DIR), CACHE_SIZE_BYTES))
            .addNetworkInterceptor(CacheLifetimeInterceptor { remoteConfig.httpCacheLifetime })
            .callTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .build()

    @Provides
    @Singleton
    fun provideJson(): Json = Json { ignoreUnknownKeys = true }
}
