package by.mazokaleh.skarnik.feature.review.domain

class FakeReviewSettings(var viewCount: Long = 0, var requested: Boolean = false) : ReviewSettings {
    var failure: Exception? = null

    override suspend fun isReviewRequested(): Boolean {
        failure?.let { throw it }
        return requested
    }

    override suspend fun incrementTranslationViewCount(): Long = ++viewCount

    override suspend fun markReviewRequested() {
        requested = true
    }
}
