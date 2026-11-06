package by.mazokaleh.skarnik.feature.whatsnew.ui

import app.cash.turbine.test
import by.mazokaleh.skarnik.core.analytics.ConsentState
import by.mazokaleh.skarnik.core.analytics.FakeAnalyticsConsent
import by.mazokaleh.skarnik.feature.whatsnew.domain.FakeInstallInfo
import by.mazokaleh.skarnik.feature.whatsnew.domain.FakeWhatsNewSettings
import by.mazokaleh.skarnik.feature.whatsnew.domain.GetWhatsNewUseCase
import by.mazokaleh.skarnik.feature.whatsnew.domain.release
import kotlin.test.assertEquals
import kotlin.test.assertNull
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
class WhatsNewViewModelTest {
    private val answered = ConsentState(answered = true, granted = false)

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun getWhatsNew(updated: Boolean) =
        GetWhatsNewUseCase(FakeWhatsNewSettings(), FakeInstallInfo("4.0.0", updated), listOf(release("4.0.0")))

    @Test
    fun `shows the notes after an update until dismissed`() = runTest {
        val viewModel = WhatsNewViewModel(getWhatsNew(updated = true), FakeAnalyticsConsent(answered))

        viewModel.release.test {
            assertEquals("4.0.0", awaitItem()?.version)
            viewModel.dismiss()
            assertNull(awaitItem())
        }
    }

    @Test
    fun `waits for the consent answer`() = runTest {
        val consent = FakeAnalyticsConsent()
        val viewModel = WhatsNewViewModel(getWhatsNew(updated = true), consent)

        viewModel.release.test {
            assertNull(awaitItem())
            consent.setGranted(false)
            assertEquals("4.0.0", awaitItem()?.version)
        }
    }

    @Test
    fun `shows nothing on a fresh install`() = runTest {
        val viewModel = WhatsNewViewModel(getWhatsNew(updated = false), FakeAnalyticsConsent(answered))

        viewModel.release.test {
            assertNull(awaitItem())
            expectNoEvents()
        }
    }
}
