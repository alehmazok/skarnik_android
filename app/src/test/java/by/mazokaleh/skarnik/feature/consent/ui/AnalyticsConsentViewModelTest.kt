package by.mazokaleh.skarnik.feature.consent.ui

import app.cash.turbine.test
import by.mazokaleh.skarnik.core.analytics.ConsentState
import by.mazokaleh.skarnik.core.analytics.FakeAnalyticsConsent
import kotlin.test.assertEquals
import kotlin.test.assertFalse
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
class AnalyticsConsentViewModelTest {
    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `asks until the user answers, then saves the answer`() = runTest {
        val consent = FakeAnalyticsConsent()
        val viewModel = AnalyticsConsentViewModel(consent)

        viewModel.showDialog.test {
            // false while the answer is read, then true for a first launch.
            if (!awaitItem()) assertTrue(awaitItem())
            viewModel.answer(granted = false)
            assertFalse(awaitItem())
        }
        assertEquals(ConsentState(answered = true, granted = false), consent.stored.value)
    }

    @Test
    fun `does not ask someone who already answered`() = runTest {
        val viewModel = AnalyticsConsentViewModel(FakeAnalyticsConsent(ConsentState(answered = true, granted = true)))

        viewModel.showDialog.test {
            assertFalse(awaitItem())
            expectNoEvents()
        }
    }
}
