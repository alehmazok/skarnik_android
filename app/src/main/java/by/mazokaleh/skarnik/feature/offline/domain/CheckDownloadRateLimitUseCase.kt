package by.mazokaleh.skarnik.feature.offline.domain

import by.mazokaleh.skarnik.core.common.WallClock
import javax.inject.Inject
import kotlin.time.Duration.Companion.minutes

/**
 * At most [MAX_ATTEMPTS] download starts per [WINDOW], so repeated taps cannot hammer Supabase
 * (Flutter's `CheckDownloadRateLimitUseCase`).
 */
class CheckDownloadRateLimitUseCase @Inject constructor(
    private val settings: OfflineDictionarySettings,
    private val clock: WallClock
) {
    /** True, and the attempt is recorded, if a download may start now. False (not recorded) otherwise. */
    suspend operator fun invoke(): Boolean {
        val now = clock.nowMillis()
        val windowStart = now - WINDOW.inWholeMilliseconds
        val recent = settings.recentDownloadAttempts().filter { it > windowStart }
        if (recent.size >= MAX_ATTEMPTS) {
            // Old entries are dropped even when blocked, so they don't linger in storage.
            settings.setRecentDownloadAttempts(recent)
            return false
        }
        settings.setRecentDownloadAttempts(recent + now)
        return true
    }

    companion object {
        val WINDOW = 5.minutes
        const val MAX_ATTEMPTS = 4
    }
}
