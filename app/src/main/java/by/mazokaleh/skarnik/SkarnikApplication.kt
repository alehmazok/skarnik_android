package by.mazokaleh.skarnik

import android.app.Application
import android.util.Log
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import by.mazokaleh.skarnik.core.analytics.AnalyticsConsent
import by.mazokaleh.skarnik.core.common.suspendRunCatching
import by.mazokaleh.skarnik.core.config.RemoteConfig
import by.mazokaleh.skarnik.core.network.FlutterHttpCacheCleaner
import by.mazokaleh.skarnik.feature.widget.data.WordOfTheDayWork
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch

@HiltAndroidApp
class SkarnikApplication :
    Application(),
    Configuration.Provider {
    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    @Inject
    lateinit var flutterHttpCacheCleaner: FlutterHttpCacheCleaner

    @Inject
    lateinit var analyticsConsent: AnalyticsConsent

    @Inject
    lateinit var remoteConfig: RemoteConfig

    /** WorkManager starts on first use with this configuration, so workers get their dependencies from Hilt. */
    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder().setWorkerFactory(workerFactory).build()

    override fun onCreate() {
        super.onCreate()
        // Enqueues a database write on WorkManager's own thread; nothing waits for it.
        WordOfTheDayWork.cancelFlutterJobs(this)
        // The cleaner switches to the IO dispatcher; the app scope lives as long as the process.
        MainScope().launch { flutterHttpCacheCleaner() }
        // DataStore reads on its own IO scope. Collection is off by default (manifest) until this applies a "yes".
        MainScope().launch {
            suspendRunCatching { analyticsConsent.applyStored() }
                .onFailure { Log.e("SkarnikApplication", "Cannot apply the analytics consent", it) }
        }
        // Nothing waits for it (Flutter didn't either): until it succeeds, the last activated values or the defaults
        // apply. A failure (offline, throttled) is retried at the next start.
        MainScope().launch {
            suspendRunCatching { remoteConfig.fetchAndActivate() }
                .onFailure { Log.w("SkarnikApplication", "Remote Config fetch failed", it) }
        }
    }
}
