package by.mazokaleh.skarnik.core.analytics

import android.content.Context
import android.os.Bundle
import android.util.Log
import by.mazokaleh.skarnik.BuildConfig
import com.google.firebase.analytics.FirebaseAnalytics
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Usage analytics. Calls return at once and never throw: the SDK queues the work on its own thread, so callers
 * fire and forget (Flutter: never await analytics). Nothing is sent until the user allows it ([AnalyticsConsent]).
 */
interface Analytics {
    fun log(event: AnalyticsEvent)

    fun setCollectionEnabled(enabled: Boolean)
}

/** Firebase Analytics. Debug builds have collection deactivated in the manifest and only log to Logcat. */
@Singleton
class FirebaseAnalyticsTracker @Inject constructor(@ApplicationContext private val context: Context) : Analytics {
    private val firebase by lazy { FirebaseAnalytics.getInstance(context) }

    override fun log(event: AnalyticsEvent) {
        if (BuildConfig.DEBUG) Log.d(TAG, "${event.name} ${event.params}")
        runCatching { firebase.logEvent(event.name, event.params.toBundle()) }
            .onFailure { Log.w(TAG, "Cannot log ${event.name}", it) }
    }

    override fun setCollectionEnabled(enabled: Boolean) {
        if (BuildConfig.DEBUG) Log.d(TAG, "collection enabled: $enabled")
        runCatching { firebase.setAnalyticsCollectionEnabled(enabled) }
            .onFailure { Log.w(TAG, "Cannot change analytics collection", it) }
    }

    private fun Map<String, Any>.toBundle(): Bundle = Bundle().apply {
        forEach { (key, value) ->
            when (value) {
                is Long -> putLong(key, value)
                else -> putString(key, value.toString())
            }
        }
    }

    private companion object {
        const val TAG = "Analytics"
    }
}

@Module
@InstallIn(SingletonComponent::class)
interface AnalyticsModule {
    @Binds
    fun bindAnalytics(impl: FirebaseAnalyticsTracker): Analytics
}
