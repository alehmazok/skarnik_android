package by.mazokaleh.skarnik.feature.update.ui

import android.app.Activity
import by.mazokaleh.skarnik.core.analytics.AnalyticsEvent
import by.mazokaleh.skarnik.core.analytics.ConsentState
import by.mazokaleh.skarnik.core.analytics.FakeAnalytics
import by.mazokaleh.skarnik.core.analytics.FakeAnalyticsConsent
import by.mazokaleh.skarnik.core.config.FakeRemoteConfig
import by.mazokaleh.skarnik.feature.update.domain.AppUpdateType
import by.mazokaleh.skarnik.feature.update.domain.ChooseAppUpdateUseCase
import by.mazokaleh.skarnik.feature.update.domain.FakeAppUpdateSettings
import by.mazokaleh.skarnik.feature.update.domain.FakeAppUpdates
import by.mazokaleh.skarnik.feature.update.domain.NoOpLauncher
import by.mazokaleh.skarnik.feature.update.domain.status
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AppUpdateViewModelTest {
    private val updates = FakeAppUpdates()
    private val analytics = FakeAnalytics()
    private val consent = FakeAnalyticsConsent(ConsentState(answered = true, granted = true))

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun viewModel(minSupported: Long = 0) = AppUpdateViewModel(
        updates,
        ChooseAppUpdateUseCase(FakeAppUpdateSettings(), FakeRemoteConfig(minSupportedVersionCode = minSupported), 171),
        consent,
        analytics
    )

    @Test
    fun `starts the flexible prompt and logs the answer`() = runTest {
        val viewModel = viewModel()
        viewModel.onResume()
        assertEquals(AppUpdateType.Flexible, viewModel.request.value)

        viewModel.launch(AppUpdateType.Flexible, NoOpLauncher)
        assertNull(viewModel.request.value)
        assertEquals(listOf(AppUpdateType.Flexible), updates.started)
        viewModel.onFlowResult(Activity.RESULT_OK)

        assertEquals(
            listOf(
                AnalyticsEvent.AppUpdatePromptShown(AppUpdateType.Flexible, 180),
                AnalyticsEvent.AppUpdateAccepted(AppUpdateType.Flexible, 180)
            ),
            analytics.events
        )
    }

    @Test
    fun `logs a dismissed prompt and does not show it again`() = runTest {
        val viewModel = viewModel()
        viewModel.onResume()
        viewModel.launch(AppUpdateType.Flexible, NoOpLauncher)
        viewModel.onFlowResult(Activity.RESULT_CANCELED)
        viewModel.onResume()

        assertNull(viewModel.request.value)
        assertEquals(AnalyticsEvent.AppUpdateDismissed(AppUpdateType.Flexible, 180), analytics.events.last())
    }

    @Test
    fun `waits for the consent answer`() = runTest {
        val unanswered = FakeAnalyticsConsent()
        val viewModel = AppUpdateViewModel(
            updates,
            ChooseAppUpdateUseCase(FakeAppUpdateSettings(), FakeRemoteConfig(), 171),
            unanswered,
            analytics
        )
        viewModel.onResume()
        assertEquals(0, updates.checks)

        unanswered.setGranted(false)
        assertEquals(1, updates.checks)
        assertEquals(AppUpdateType.Flexible, viewModel.request.value)
    }

    @Test
    fun `logs nothing when Play cannot start the flow`() = runTest {
        updates.canStart = false
        val viewModel = viewModel()
        viewModel.onResume()
        viewModel.launch(AppUpdateType.Flexible, NoOpLauncher)
        viewModel.onFlowResult(Activity.RESULT_CANCELED)

        assertTrue(analytics.events.isEmpty())
    }

    @Test
    fun `a closed immediate flow is not restarted in the same session`() = runTest {
        updates.status = status(flexibleAllowed = false)
        val viewModel = viewModel(minSupported = 172)
        viewModel.onResume()
        viewModel.launch(AppUpdateType.Immediate, NoOpLauncher)
        viewModel.onFlowResult(Activity.RESULT_CANCELED)
        viewModel.onResume()

        assertNull(viewModel.request.value)
        assertEquals(AnalyticsEvent.AppUpdateDismissed(AppUpdateType.Immediate, 180), analytics.events.last())
    }

    @Test
    fun `offers a restart after the download and installs on restart`() = runTest {
        updates.status = null
        val viewModel = viewModel()
        assertFalse(viewModel.restartReady.value)

        updates.downloads.emit(Unit)
        assertTrue(viewModel.restartReady.value)
        viewModel.restart()

        assertFalse(viewModel.restartReady.value)
        assertEquals(1, updates.completed)
        assertEquals(AnalyticsEvent.AppUpdateRestartClicked, analytics.events.single())
    }

    @Test
    fun `a downloaded update found by the check is offered again after a dismissal`() = runTest {
        updates.status = status(available = false, downloaded = true)
        val viewModel = viewModel()
        viewModel.onResume()
        assertTrue(viewModel.restartReady.value)

        viewModel.restartDismissed()
        assertFalse(viewModel.restartReady.value)
        viewModel.onResume()
        assertTrue(viewModel.restartReady.value)
    }
}
