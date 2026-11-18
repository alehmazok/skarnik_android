package by.mazokaleh.skarnik.feature.update.domain

import by.mazokaleh.skarnik.core.config.RemoteConfig
import javax.inject.Inject
import javax.inject.Qualifier

/** The running app's `versionCode`. */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class AppVersionCode

/** What to do with an update Play reports. */
sealed interface AppUpdateAction {
    data object None : AppUpdateAction

    data class Start(val type: AppUpdateType) : AppUpdateAction

    /** A flexible update is downloaded: offer to restart the app. */
    data object OfferRestart : AppUpdateAction
}

/**
 * Decides between the flows (docs/adr/0016):
 * - a downloaded update is offered for a restart, every time the app comes to the front until it is installed;
 * - below Remote Config's `min_supported_version_code` the immediate flow starts, also to resume one the user left,
 *   unless the user closed it in this session ([immediateDeclined]);
 * - otherwise the flexible prompt shows once per available version. The version is stored before the prompt
 *   shows (as the review request and "what's new"), so it shows at most once even if the app is closed meanwhile.
 */
class ChooseAppUpdateUseCase @Inject constructor(
    private val settings: AppUpdateSettings,
    private val remoteConfig: RemoteConfig,
    @AppVersionCode private val currentVersionCode: Long
) {
    suspend operator fun invoke(status: AppUpdateStatus, immediateDeclined: Boolean): AppUpdateAction {
        if (status.downloaded) return AppUpdateAction.OfferRestart
        val immediateRequired = currentVersionCode < remoteConfig.minSupportedVersionCode && !immediateDeclined
        val immediatePossible = status.inProgress || status.available && status.immediateAllowed
        return when {
            immediateRequired && immediatePossible -> AppUpdateAction.Start(AppUpdateType.Immediate)
            !status.available || !status.flexibleAllowed -> AppUpdateAction.None
            status.availableVersionCode <= (settings.promptedVersionCode() ?: 0L) -> AppUpdateAction.None
            else -> {
                settings.setPromptedVersionCode(status.availableVersionCode)
                AppUpdateAction.Start(AppUpdateType.Flexible)
            }
        }
    }
}
