package by.mazokaleh.skarnik.feature.whatsnew.domain

import by.mazokaleh.skarnik.core.common.suspendRunCatching
import by.mazokaleh.skarnik.core.crash.logError
import by.mazokaleh.skarnik.feature.whatsnew.data.WhatsNewCatalog
import javax.inject.Inject

/**
 * Which notes to show at this start, if any. The notes show once, on the first start of a version that has them,
 * and never on a fresh install. Whatever is returned is already marked seen, so the sheet shows at most once even
 * if the app is closed while it is open. Storage errors mean "show nothing".
 */
class GetWhatsNewUseCase internal constructor(
    private val settings: WhatsNewSettings,
    private val installInfo: InstallInfo,
    private val releases: List<WhatsNewRelease>
) {
    @Inject
    constructor(settings: WhatsNewSettings, installInfo: InstallInfo) :
        this(settings, installInfo, WhatsNewCatalog.releases)

    suspend operator fun invoke(): WhatsNewRelease? = suspendRunCatching {
        val current = installInfo.currentVersion
        val lastSeen = settings.lastSeenVersion()
        val release = if (lastSeen == null && !installInfo.wasUpdated()) {
            null
        } else {
            releases.unseen(current, lastSeen)
        }
        settings.setLastSeenVersion(current)
        release
    }.getOrElse {
        logError(TAG, "Cannot read what's new", it)
        null
    }

    private companion object {
        const val TAG = "WhatsNew"
    }
}
