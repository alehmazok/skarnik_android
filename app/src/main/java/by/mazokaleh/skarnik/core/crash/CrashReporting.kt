package by.mazokaleh.skarnik.core.crash

import android.content.Context
import android.util.Log
import by.mazokaleh.skarnik.BuildConfig
import io.sentry.Sentry
import io.sentry.SentryOptions
import io.sentry.android.core.SentryAndroid
import io.sentry.android.core.SentryAndroidOptions
import java.io.IOException

/**
 * Crash reporting with Sentry (parity 0.6, docs/adr/0008), release builds only. Crashes, ANRs and native crashes are
 * reported by the SDK; [logError] adds non-fatal errors, as Flutter recorded its SEVERE log records.
 */
object CrashReporting {
    /** Starts Sentry before anything else in the app runs. Does nothing without a DSN (debug and local builds). */
    fun start(context: Context, config: CrashReportingConfig = CrashReportingConfig.fromBuildConfig()) {
        if (!config.isEnabled) return
        SentryAndroid.init(context) { options: SentryAndroidOptions ->
            configure(options, config)
            // No screenshots, view hierarchy or tap breadcrumbs: the screen can show what the user searched for.
            options.isAttachScreenshot = false
            options.isAttachViewHierarchy = false
            options.isEnableUserInteractionBreadcrumbs = false
        }
    }

    /** The platform-independent options; kept apart so a JVM test can check them. */
    internal fun configure(options: SentryOptions, config: CrashReportingConfig) {
        options.dsn = config.dsn
        options.environment = config.environment
        config.proguardUuid.takeIf { it.isNotBlank() }?.let { options.proguardUuid = it }
        // Nothing that identifies the user: no IP address, no user id. Errors only, no performance tracing.
        options.isSendDefaultPii = false
        options.tracesSampleRate = null
    }

    /**
     * Whether a caught error is worth a report. Network and HTTP failures ([IOException]) are expected on a phone
     * (offline, a server down) and would bury real bugs, so they stay in Logcat only.
     */
    fun isReportable(error: Throwable): Boolean = error !is IOException
}

data class CrashReportingConfig(val dsn: String, val environment: String, val proguardUuid: String = "") {
    val isEnabled: Boolean get() = dsn.isNotBlank()

    companion object {
        fun fromBuildConfig() = CrashReportingConfig(
            dsn = BuildConfig.SENTRY_DSN,
            environment = BuildConfig.SENTRY_ENVIRONMENT,
            proguardUuid = BuildConfig.SENTRY_PROGUARD_UUID
        )
    }
}

/**
 * Logs a caught error to Logcat and reports it to Sentry as a non-fatal when [CrashReporting.isReportable].
 * Only the exception and [tag] are sent, not [message]: messages can hold the user's query or word.
 * A no-op for Sentry while it is not started (debug builds, unit tests).
 */
fun logError(tag: String, message: String, error: Throwable) {
    Log.e(tag, message, error)
    if (CrashReporting.isReportable(error)) {
        Sentry.captureException(error) { scope -> scope.setTag("log_tag", tag) }
    }
}
