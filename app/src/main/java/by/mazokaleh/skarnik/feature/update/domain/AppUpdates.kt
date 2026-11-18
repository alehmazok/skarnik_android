package by.mazokaleh.skarnik.feature.update.domain

import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import kotlinx.coroutines.flow.Flow

/** Google Play's two in-app update flows. */
enum class AppUpdateType(val analyticsName: String) {
    /** Downloads in the background while the app is used; the app restarts when the user says so. */
    Flexible("flexible"),

    /** Play's full-screen flow: the app is blocked until the update is installed. */
    Immediate("immediate")
}

/** What Play knows about a newer version of the app, at one moment. */
data class AppUpdateStatus(
    /** True when Play has a newer version this device can install. */
    val available: Boolean,
    val availableVersionCode: Long,
    val flexibleAllowed: Boolean,
    val immediateAllowed: Boolean,
    /** An update started by the app is still running (a download, or an immediate flow the user left). */
    val inProgress: Boolean,
    /** A flexible update has been downloaded and waits for a restart to install. */
    val downloaded: Boolean
)

/** Google Play In-App Updates. Without Play, or for an app not installed from Play, nothing is ever available. */
interface AppUpdates {
    /** The current status, or null when Play cannot tell (no Play Store, a sideloaded app, an error). */
    suspend fun check(): AppUpdateStatus?

    /** Emits each time a flexible update finishes downloading. */
    val downloaded: Flow<Unit>

    /**
     * Starts [type]'s flow for the update found by the last [check]; the result comes back to [launcher]. False
     * when it cannot start (no check yet, or that status was already used: Play allows one start per check).
     */
    fun start(type: AppUpdateType, launcher: ActivityResultLauncher<IntentSenderRequest>): Boolean

    /** Installs a downloaded flexible update; Play restarts the app. */
    fun completeUpdate()
}

/** Which available version the flexible prompt was last shown for, so it is shown once per version. */
interface AppUpdateSettings {
    suspend fun promptedVersionCode(): Long?

    suspend fun setPromptedVersionCode(versionCode: Long)
}
