package by.mazokaleh.skarnik.feature.offline.domain

import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest
import org.junit.Test

class CheckDownloadRateLimitUseCaseTest {
    private val settings = FakeOfflineDictionarySettings()
    private var now = 1_000_000_000L
    private val check = CheckDownloadRateLimitUseCase(settings) { now }
    private val window = CheckDownloadRateLimitUseCase.WINDOW.inWholeMilliseconds

    @Test
    fun `allows four starts in five minutes, then blocks without recording`() = runTest {
        repeat(CheckDownloadRateLimitUseCase.MAX_ATTEMPTS) {
            assertTrue(check())
            now += 1_000
        }

        assertFalse(check())
        assertEquals(CheckDownloadRateLimitUseCase.MAX_ATTEMPTS, settings.attempts.size)
    }

    @Test
    fun `attempts older than the window no longer count and are dropped`() = runTest {
        settings.attempts = List(CheckDownloadRateLimitUseCase.MAX_ATTEMPTS) { now - window - it }

        assertTrue(check())
        assertEquals(listOf(now), settings.attempts)
    }

    @Test
    fun `old attempts are dropped even when blocked`() = runTest {
        val recent = List(CheckDownloadRateLimitUseCase.MAX_ATTEMPTS) { now - it }
        settings.attempts = recent + (now - window - 1)

        assertFalse(check())
        assertEquals(recent, settings.attempts)
    }
}
