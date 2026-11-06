package by.mazokaleh.skarnik.feature.whatsnew.domain

import java.io.IOException
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlinx.coroutines.test.runTest
import org.junit.Test

class GetWhatsNewUseCaseTest {
    private val releases = listOf(release("4.1.0"), release("4.0.0"))

    private fun useCase(settings: WhatsNewSettings, installInfo: InstallInfo) =
        GetWhatsNewUseCase(settings, installInfo, releases)

    @Test
    fun `a fresh install shows nothing and remembers the version`() = runTest {
        val settings = FakeWhatsNewSettings()

        assertNull(useCase(settings, FakeInstallInfo("4.0.0", updated = false))())
        assertEquals("4.0.0", settings.lastSeen)
    }

    @Test
    fun `the update from the Flutter app shows the notes once`() = runTest {
        val settings = FakeWhatsNewSettings()
        val getWhatsNew = useCase(settings, FakeInstallInfo("4.0.0", updated = true))

        assertEquals("4.0.0", getWhatsNew()?.version)
        assertNull(getWhatsNew())
        assertEquals("4.0.0", settings.lastSeen)
    }

    @Test
    fun `an update shows the new version's notes`() = runTest {
        val settings = FakeWhatsNewSettings(lastSeen = "4.0.0")

        assertEquals("4.1.0", useCase(settings, FakeInstallInfo("4.1.0", updated = true))()?.version)
        assertEquals("4.1.0", settings.lastSeen)
    }

    @Test
    fun `a version without notes shows nothing and is remembered`() = runTest {
        val settings = FakeWhatsNewSettings(lastSeen = "4.1.0")

        assertNull(useCase(settings, FakeInstallInfo("4.1.1", updated = true))())
        assertEquals("4.1.1", settings.lastSeen)
    }

    @Test
    fun `a storage error shows nothing`() = runTest {
        val settings = FakeWhatsNewSettings().apply { failure = IOException("disk") }

        assertNull(useCase(settings, FakeInstallInfo("4.0.0", updated = true))())
    }
}
