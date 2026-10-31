package by.mazokaleh.skarnik.feature.review.domain

import java.io.IOException
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest
import org.junit.Test

class ShouldAskForReviewUseCaseTest {
    private val settings = FakeReviewSettings()
    private val shouldAsk = ShouldAskForReviewUseCase(settings)

    @Test
    fun `asks on the tenth translation view, once`() = runTest {
        val answers = List(15) { shouldAsk() }

        assertEquals(List(9) { false } + true + List(5) { false }, answers)
        assertTrue(settings.requested)
        // Counting stops once asked, as in Flutter.
        assertEquals(10, settings.viewCount)
    }

    @Test
    fun `never asks when the Flutter app already asked`() = runTest {
        settings.requested = true
        settings.viewCount = 3

        assertFalse(shouldAsk())
        assertEquals(3, settings.viewCount)
    }

    @Test
    fun `a migrated count carries on`() = runTest {
        settings.viewCount = 8

        assertFalse(shouldAsk())
        assertTrue(shouldAsk())
    }

    @Test
    fun `a storage error means not asking`() = runTest {
        settings.failure = IOException("disk")

        assertFalse(shouldAsk())
        assertFalse(settings.requested)
    }
}
