package by.mazokaleh.skarnik.feature.update.domain

import by.mazokaleh.skarnik.core.config.FakeRemoteConfig
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlinx.coroutines.test.runTest
import org.junit.Test

class ChooseAppUpdateUseCaseTest {
    private val settings = FakeAppUpdateSettings()

    private fun choose(minSupported: Long = 0) =
        ChooseAppUpdateUseCase(settings, FakeRemoteConfig(minSupportedVersionCode = minSupported), CURRENT)

    @Test
    fun `offers the flexible update once per version`() = runTest {
        val choose = choose()

        assertEquals(AppUpdateAction.Start(AppUpdateType.Flexible), choose(status(versionCode = 180), false))
        assertEquals(180L, settings.prompted)
        assertEquals(AppUpdateAction.None, choose(status(versionCode = 180), false))
        assertEquals(AppUpdateAction.Start(AppUpdateType.Flexible), choose(status(versionCode = 181), false))
    }

    @Test
    fun `nothing without an update or when Play does not allow the flexible flow`() = runTest {
        val choose = choose()

        assertEquals(AppUpdateAction.None, choose(status(available = false), false))
        assertEquals(AppUpdateAction.None, choose(status(flexibleAllowed = false), false))
        assertNull(settings.prompted)
    }

    @Test
    fun `a downloaded update is offered for a restart every time`() = runTest {
        settings.prompted = 180
        val choose = choose(minSupported = 999)

        assertEquals(AppUpdateAction.OfferRestart, choose(status(available = false, downloaded = true), false))
        assertEquals(AppUpdateAction.OfferRestart, choose(status(available = false, downloaded = true), false))
    }

    @Test
    fun `below the minimum the immediate flow starts at every check`() = runTest {
        val choose = choose(minSupported = CURRENT + 1)

        assertEquals(AppUpdateAction.Start(AppUpdateType.Immediate), choose(status(), false))
        assertEquals(AppUpdateAction.Start(AppUpdateType.Immediate), choose(status(), false))
        assertNull(settings.prompted)
    }

    @Test
    fun `below the minimum an immediate flow the user left is resumed`() = runTest {
        val inProgress = status(available = false, immediateAllowed = false, inProgress = true)

        val required = choose(minSupported = CURRENT + 1)
        assertEquals(AppUpdateAction.Start(AppUpdateType.Immediate), required(inProgress, false))
        // At or above the minimum, an update in progress is a flexible download: it finishes on its own.
        assertEquals(AppUpdateAction.None, choose(minSupported = CURRENT)(inProgress, false))
    }

    @Test
    fun `after the user closed the immediate flow the flexible prompt is offered instead`() = runTest {
        val choose = choose(minSupported = CURRENT + 1)

        assertEquals(AppUpdateAction.Start(AppUpdateType.Flexible), choose(status(), immediateDeclined = true))
        assertEquals(AppUpdateAction.None, choose(status(), immediateDeclined = true))
    }

    @Test
    fun `below the minimum without the immediate flow the flexible one is offered`() = runTest {
        val choose = choose(minSupported = CURRENT + 1)

        assertEquals(AppUpdateAction.Start(AppUpdateType.Flexible), choose(status(immediateAllowed = false), false))
    }

    private companion object {
        const val CURRENT = 171L
    }
}
