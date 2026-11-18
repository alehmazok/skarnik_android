package by.mazokaleh.skarnik.core.network

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import java.io.File
import java.util.concurrent.TimeUnit
import javax.inject.Singleton
import kotlin.time.Duration.Companion.hours
import kotlinx.serialization.json.Json
import okhttp3.Cache
import okhttp3.OkHttpClient

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {
    private const val CACHE_DIR = "http"
    private const val CACHE_SIZE_BYTES = 20L * 1024 * 1024
    private const val TIMEOUT_SECONDS = 20L

    /**
     * How long fetched translations and stress tables are served from the HTTP cache. A constant, not Flutter's
     * Remote Config parameter: dictionary edits are rare, a day of staleness is fine, and a changed value could not
     * shorten responses already cached (their max-age is written when they are stored).
     */
    private val CACHE_LIFETIME = 24.hours

    /** One client for the whole app. The disk cache is created lazily by OkHttp, off the main thread. */
    @Provides
    @Singleton
    fun provideOkHttpClient(@ApplicationContext context: Context): OkHttpClient = OkHttpClient.Builder()
        .cache(Cache(File(context.cacheDir, CACHE_DIR), CACHE_SIZE_BYTES))
        .addNetworkInterceptor(CacheLifetimeInterceptor(CACHE_LIFETIME))
        .callTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .build()

    @Provides
    @Singleton
    fun provideJson(): Json = Json { ignoreUnknownKeys = true }
}
