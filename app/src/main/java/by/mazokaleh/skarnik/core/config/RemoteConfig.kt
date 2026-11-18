package by.mazokaleh.skarnik.core.config

import android.util.Log
import by.mazokaleh.skarnik.core.common.IoDispatcher
import by.mazokaleh.skarnik.core.common.await
import com.google.firebase.remoteconfig.ConfigUpdate
import com.google.firebase.remoteconfig.ConfigUpdateListener
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.FirebaseRemoteConfigException
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings
import com.google.firebase.remoteconfig.FirebaseRemoteConfigValue
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext

/**
 * Values the app takes from Firebase Remote Config (parity 8.8). Reads never fail and fall back to the built-in
 * defaults. Flutter's `http_cache_duration_in_hours` is not read: the HTTP cache lifetime is a constant
 * ([by.mazokaleh.skarnik.core.network.NetworkModule]).
 */
interface RemoteConfig {
    /** Where a translation shows the link to the same word in the other dictionary (Бел-Рус ⇄ ТСБМ); an A/B test. */
    val dictionaryLinkPlacement: DictionaryLinkPlacement

    /**
     * Fetches new values and activates them at once, as Flutter did, and from then on activates values published in
     * the console as they arrive (real-time updates), so they don't wait for the 12-hour throttle and a cold start.
     * Throws when the fetch fails; real-time updates are registered even then.
     */
    suspend fun fetchAndActivate()
}

@Singleton
class FirebaseAppRemoteConfig @Inject constructor(@IoDispatcher private val ioDispatcher: CoroutineDispatcher) :
    RemoteConfig {
    // getInstance reads Firebase's metadata from disk, so it is created on first use, never at injection.
    private val config by lazy { FirebaseRemoteConfig.getInstance() }

    /** Read when a translation loads, so a new value applies from the next word opened. */
    override val dictionaryLinkPlacement: DictionaryLinkPlacement
        get() = dictionaryLinkPlacementOf(config.getValue(DICTIONARY_LINK_PLACEMENT))

    private val listening = AtomicBoolean(false)

    override suspend fun fetchAndActivate() {
        withContext(ioDispatcher) {
            config.setConfigSettingsAsync(SETTINGS).await()
            listenForUpdates()
            config.fetchAndActivate().await()
        }
    }

    /** Once per process: the backend pushes a change as soon as it is published, and it applies at once. */
    private fun listenForUpdates() {
        if (!listening.compareAndSet(false, true)) return
        config.addOnConfigUpdateListener(object : ConfigUpdateListener {
            override fun onUpdate(configUpdate: ConfigUpdate) {
                config.activate()
            }

            override fun onError(error: FirebaseRemoteConfigException) {
                Log.w(TAG, "Remote Config real-time updates failed", error)
            }
        })
    }

    companion object {
        private const val TAG = "RemoteConfig"

        /** New in the native apps (skarnik_ios reads the same parameter): `header`, `footer` or `none`. */
        const val DICTIONARY_LINK_PLACEMENT = "dictionary_link_placement"

        // Flutter's settings: a fetch waits up to a minute and runs at most every 12 hours.
        private val SETTINGS = FirebaseRemoteConfigSettings.Builder()
            .setFetchTimeoutInSeconds(1.minutes.inWholeSeconds)
            .setMinimumFetchIntervalInSeconds(12.hours.inWholeSeconds)
            .build()
    }
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
