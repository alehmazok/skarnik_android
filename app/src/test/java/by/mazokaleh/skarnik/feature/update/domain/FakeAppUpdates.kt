package by.mazokaleh.skarnik.feature.update.domain

import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContract
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityOptionsCompat
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow

fun status(
    available: Boolean = true,
    versionCode: Long = 180,
    flexibleAllowed: Boolean = true,
    immediateAllowed: Boolean = true,
    inProgress: Boolean = false,
    downloaded: Boolean = false
) = AppUpdateStatus(available, versionCode, flexibleAllowed, immediateAllowed, inProgress, downloaded)

class FakeAppUpdates(var status: AppUpdateStatus? = status()) : AppUpdates {
    val started = mutableListOf<AppUpdateType>()
    var checks = 0
    var completed = 0
    var canStart = true
    val downloads = MutableSharedFlow<Unit>(extraBufferCapacity = 1)

    override suspend fun check(): AppUpdateStatus? {
        checks++
        return status
    }

    override val downloaded: Flow<Unit> = downloads

    override fun start(type: AppUpdateType, launcher: ActivityResultLauncher<IntentSenderRequest>): Boolean {
        if (canStart) started += type
        return canStart
    }

    override fun completeUpdate() {
        completed++
    }
}

class FakeAppUpdateSettings(var prompted: Long? = null) : AppUpdateSettings {
    override suspend fun promptedVersionCode(): Long? = prompted

    override suspend fun setPromptedVersionCode(versionCode: Long) {
        prompted = versionCode
    }
}

/** A launcher that launches nothing: the fake [AppUpdates] never uses it. */
object NoOpLauncher : ActivityResultLauncher<IntentSenderRequest>() {
    override val contract: ActivityResultContract<IntentSenderRequest, *> =
        ActivityResultContracts.StartIntentSenderForResult()

    override fun launch(input: IntentSenderRequest, options: ActivityOptionsCompat?) = Unit

    override fun unregister() = Unit
}
