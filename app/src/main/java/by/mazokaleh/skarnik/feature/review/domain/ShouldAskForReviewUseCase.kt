package by.mazokaleh.skarnik.feature.review.domain

import android.util.Log
import by.mazokaleh.skarnik.core.common.suspendRunCatching
import javax.inject.Inject

/**
 * Flutter's `CheckAndRequestReviewUseCase` without the prompt itself: counts a translation view and says whether
 * this is the moment to ask for a review (the [THRESHOLD]th view, once per install).
 *
 * The request is marked before the caller shows anything, as in Flutter, so people are asked at most once even if
 * the prompt fails or the app is closed meanwhile. Storage errors mean "don't ask".
 */
class ShouldAskForReviewUseCase @Inject constructor(private val settings: ReviewSettings) {
    suspend operator fun invoke(): Boolean = suspendRunCatching {
        if (settings.isReviewRequested()) return false
        if (settings.incrementTranslationViewCount() < THRESHOLD) return false
        settings.markReviewRequested()
        true
    }.getOrElse {
        Log.w(TAG, "Cannot check whether to ask for a review", it)
        false
    }

    companion object {
        /** Flutter's `AppConfig.reviewRequestViewThreshold`. */
        const val THRESHOLD = 10L
        private const val TAG = "Review"
    }
}
