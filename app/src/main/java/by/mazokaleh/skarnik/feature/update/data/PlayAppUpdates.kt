package by.mazokaleh.skarnik.feature.update.data

import android.content.Context
import android.util.Log
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import by.mazokaleh.skarnik.BuildConfig
import by.mazokaleh.skarnik.core.common.await
import by.mazokaleh.skarnik.core.common.suspendRunCatching
import by.mazokaleh.skarnik.feature.update.domain.AppUpdateSettings
import by.mazokaleh.skarnik.feature.update.domain.AppUpdateStatus
import by.mazokaleh.skarnik.feature.update.domain.AppUpdateType
import by.mazokaleh.skarnik.feature.update.domain.AppUpdates
import by.mazokaleh.skarnik.feature.update.domain.AppVersionCode
import com.google.android.play.core.appupdate.AppUpdateInfo
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.appupdate.AppUpdateOptions
import com.google.android.play.core.install.InstallStateUpdatedListener
import com.google.android.play.core.install.model.AppUpdateType.FLEXIBLE
import com.google.android.play.core.install.model.AppUpdateType.IMMEDIATE
import com.google.android.play.core.install.model.InstallStatus
import com.google.android.play.core.install.model.UpdateAvailability
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

/** Play In-App Updates (`com.google.android.play:app-update`). Play runs its calls on its own threads. */
@Singleton
class PlayAppUpdates @Inject constructor(@ApplicationContext private val context: Context) : AppUpdates {
    // Created on first use: the factory binds to the Play Store service.
    private val manager by lazy { AppUpdateManagerFactory.create(context) }

    /** The last checked info; Play lets each one start a flow only once. */
    @Volatile
    private var lastInfo: AppUpdateInfo? = null

    override suspend fun check(): AppUpdateStatus? = suspendRunCatching {
        val info = manager.appUpdateInfo.await()
        lastInfo = info
        info.toStatus()
    }.onFailure {
        // Expected without the Play Store, on Huawei devices, and for builds Play did not install.
        Log.i(TAG, "Play in-app updates are unavailable", it)
    }.getOrNull()

    override val downloaded: Flow<Unit> = callbackFlow {
        val listener = InstallStateUpdatedListener { state ->
            if (state.installStatus() == InstallStatus.DOWNLOADED) trySend(Unit)
        }
        manager.registerListener(listener)
        awaitClose { manager.unregisterListener(listener) }
    }

    override fun start(type: AppUpdateType, launcher: ActivityResultLauncher<IntentSenderRequest>): Boolean {
        val info = lastInfo ?: return false
        lastInfo = null
        val options = AppUpdateOptions.newBuilder(type.playType).build()
        return runCatching { manager.startUpdateFlowForResult(info, launcher, options) }
            .onFailure { Log.w(TAG, "Cannot start the ${type.analyticsName} update", it) }
            .getOrDefault(false)
    }

    override fun completeUpdate() {
        manager.completeUpdate().addOnFailureListener { Log.w(TAG, "Cannot install the update", it) }
    }

    private companion object {
        const val TAG = "AppUpdate"

        val AppUpdateType.playType: Int
            get() = when (this) {
                AppUpdateType.Flexible -> FLEXIBLE
                AppUpdateType.Immediate -> IMMEDIATE
            }

        fun AppUpdateInfo.toStatus() = AppUpdateStatus(
            available = updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE,
            availableVersionCode = availableVersionCode().toLong(),
            flexibleAllowed = isUpdateTypeAllowed(FLEXIBLE),
            immediateAllowed = isUpdateTypeAllowed(IMMEDIATE),
            inProgress = updateAvailability() == UpdateAvailability.DEVELOPER_TRIGGERED_UPDATE_IN_PROGRESS,
            downloaded = installStatus() == InstallStatus.DOWNLOADED
        )
    }
}

@Module
@InstallIn(SingletonComponent::class)
interface AppUpdateModule {
    @Binds
    fun bindAppUpdates(impl: PlayAppUpdates): AppUpdates

    @Binds
    fun bindAppUpdateSettings(impl: DataStoreAppUpdateSettings): AppUpdateSettings

    companion object {
        @Provides
        @AppVersionCode
        fun provideAppVersionCode(): Long = BuildConfig.VERSION_CODE.toLong()
    }
}
