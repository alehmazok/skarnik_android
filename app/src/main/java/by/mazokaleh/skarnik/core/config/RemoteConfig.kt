package by.mazokaleh.skarnik.core.config

import by.mazokaleh.skarnik.core.common.IoDispatcher
import by.mazokaleh.skarnik.core.common.await
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings
import com.google.firebase.remoteconfig.FirebaseRemoteConfigValue
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext

/**
 * Values the app takes from Firebase Remote Config (parity 8.8), the same parameters as Flutter's
 * `InitRemoteConfigUseCase`. Reads never fail and fall back to the built-in defaults.
 */
interface RemoteConfig {
    /** How long fetched translations and stress tables are served from the HTTP cache. */
    val httpCacheLifetime: Duration

    /** Fetches new values and activates them at once, as Flutter did. Throws when the fetch fails. */
    suspend fun fetchAndActivate()
}

@Singleton
class FirebaseAppRemoteConfig @Inject constructor(@IoDispatcher private val ioDispatcher: CoroutineDispatcher) :
    RemoteConfig {
    // getInstance reads Firebase's metadata from disk, so it is created on first use, never at injection.
    private val config by lazy { FirebaseRemoteConfig.getInstance() }

    /** Read on OkHttp's threads for every cached response, so a new value applies from the next request. */
    override val httpCacheLifetime: Duration
        get() = httpCacheLifetimeOf(config.getValue(HTTP_CACHE_DURATION_IN_HOURS))

    override suspend fun fetchAndActivate() {
        withContext(ioDispatcher) {
            config.setConfigSettingsAsync(SETTINGS).await()
            config.fetchAndActivate().await()
        }
    }

    companion object {
        /** Flutter's parameter name (`AppConfig.httpCacheDurationInHours`); the Firebase app is shared. */
        const val HTTP_CACHE_DURATION_IN_HOURS = "http_cache_duration_in_hours"

        /** Flutter's default for the parameter. */
        val DEFAULT_HTTP_CACHE_LIFETIME: Duration = 24.hours

        // Flutter's settings: a fetch waits up to a minute and runs at most every 12 hours.
        private val SETTINGS = FirebaseRemoteConfigSettings.Builder()
            .setFetchTimeoutInSeconds(1.minutes.inWholeSeconds)
            .setMinimumFetchIntervalInSeconds(12.hours.inWholeSeconds)
            .build()
    }
}

/**
 * The cache lifetime from the parameter's value. Before the first fetch, or when the parameter is missing,
 * Firebase answers with a static 0, so that means the default. 0 hours from the console turns caching off,
 * as in Flutter; a negative or non-numeric value is a console mistake and falls back to the default.
 */
internal fun httpCacheLifetimeOf(value: FirebaseRemoteConfigValue): Duration {
    val default = FirebaseAppRemoteConfig.DEFAULT_HTTP_CACHE_LIFETIME
    if (value.source == FirebaseRemoteConfig.VALUE_SOURCE_STATIC) return default
    val hours = runCatching { value.asLong() }.getOrNull()
    return if (hours == null || hours < 0) default else hours.hours
}

@Module
@InstallIn(SingletonComponent::class)
interface RemoteConfigModule {
    @Binds
    fun bindRemoteConfig(impl: FirebaseAppRemoteConfig): RemoteConfig
}
