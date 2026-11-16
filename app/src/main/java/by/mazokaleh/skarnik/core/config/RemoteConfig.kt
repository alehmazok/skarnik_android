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

    /** Where a translation shows the link to the same word in the other dictionary (Бел-Рус ⇄ ТСБМ); an A/B test. */
    val dictionaryLinkPlacement: DictionaryLinkPlacement

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

    /** Read when a translation loads, so a new value applies from the next word opened. */
    override val dictionaryLinkPlacement: DictionaryLinkPlacement
        get() = dictionaryLinkPlacementOf(config.getValue(DICTIONARY_LINK_PLACEMENT))

    override suspend fun fetchAndActivate() {
        withContext(ioDispatcher) {
            config.setConfigSettingsAsync(SETTINGS).await()
            config.fetchAndActivate().await()
        }
    }

    companion object {
        /** Flutter's parameter name (`AppConfig.httpCacheDurationInHours`); the Firebase app is shared. */
        const val HTTP_CACHE_DURATION_IN_HOURS = "http_cache_duration_in_hours"

        /** New in the native apps (skarnik_ios reads the same parameter): `header`, `footer` or `none`. */
        const val DICTIONARY_LINK_PLACEMENT = "dictionary_link_placement"

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

/** The variants of the `dictionary_link_placement` experiment, by their Remote Config value. */
enum class DictionaryLinkPlacement(val configValue: String) {
    /** A chip under the headword, beside «На́ціск». */
    Header("header"),

    /** A row after the translation. */
    Footer("footer"),

    /** No link: the experiment's control group. */
    None("none");

    companion object {
        /** What the app shows before the first fetch, without an experiment, or for a value it does not know. */
        val DEFAULT = Header
    }
}

/** The placement from the parameter's value; a missing (static), empty or unknown value means the default. */
internal fun dictionaryLinkPlacementOf(value: FirebaseRemoteConfigValue): DictionaryLinkPlacement {
    if (value.source == FirebaseRemoteConfig.VALUE_SOURCE_STATIC) return DictionaryLinkPlacement.DEFAULT
    val raw = value.asString().trim().lowercase()
    return DictionaryLinkPlacement.entries.firstOrNull { it.configValue == raw } ?: DictionaryLinkPlacement.DEFAULT
}

@Module
@InstallIn(SingletonComponent::class)
interface RemoteConfigModule {
    @Binds
    fun bindRemoteConfig(impl: FirebaseAppRemoteConfig): RemoteConfig
}
