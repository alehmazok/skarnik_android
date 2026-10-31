package by.mazokaleh.skarnik.feature.review.domain

/** Flutter's `ReviewRepository` storage half: how many translations were viewed, and whether review was asked. */
interface ReviewSettings {
    suspend fun isReviewRequested(): Boolean

    /** Adds one translation view and returns the new count. */
    suspend fun incrementTranslationViewCount(): Long

    suspend fun markReviewRequested()
}
