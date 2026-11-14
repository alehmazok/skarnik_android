package by.mazokaleh.skarnik.core.crash

import by.mazokaleh.skarnik.core.network.HttpException
import io.sentry.SentryOptions
import java.io.IOException
import java.net.UnknownHostException
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import org.junit.Test

class CrashReportingTest {
    @Test
    fun `without a DSN crash reporting is off`() {
        assertFalse(CrashReportingConfig(dsn = "", environment = "debug").isEnabled)
        assertFalse(CrashReportingConfig(dsn = "  ", environment = "production").isEnabled)
        assertTrue(CrashReportingConfig(dsn = DSN, environment = "production").isEnabled)
    }

    @Test
    fun `options carry the DSN, environment and mapping id, and nothing that identifies the user`() {
        val options = SentryOptions()

        CrashReporting.configure(options, CrashReportingConfig(DSN, "internal", MAPPING_ID))

        assertEquals(DSN, options.dsn)
        assertEquals("internal", options.environment)
        assertEquals(MAPPING_ID, options.proguardUuid)
        assertFalse(options.isSendDefaultPii)
        assertNull(options.tracesSampleRate)
    }

    @Test
    fun `Test Lab and pre-launch runs report to their own environment`() {
        assertEquals("production", CrashReportingConfig.environment("production", isTestLab = false))
        assertEquals("test-lab", CrashReportingConfig.environment("production", isTestLab = true))
        assertEquals("test-lab", CrashReportingConfig.environment("internal", isTestLab = true))
    }

    @Test
    fun `a build without a mapping id leaves it unset`() {
        val options = SentryOptions()

        CrashReporting.configure(options, CrashReportingConfig(DSN, "production"))

        assertNull(options.proguardUuid)
    }

    @Test
    fun `network and HTTP failures are not reported, other errors are`() {
        assertFalse(CrashReporting.isReportable(IOException("offline")))
        assertFalse(CrashReporting.isReportable(UnknownHostException("skarnik.play.of.by")))
        assertFalse(CrashReporting.isReportable(HttpException(503, "https://skarnik.play.of.by/api/words/")))
        assertTrue(CrashReporting.isReportable(IllegalStateException("Empty translation")))
        assertTrue(CrashReporting.isReportable(NoSuchElementException("No belrus/1 in Supabase")))
    }

    @Test
    fun `logging an error while Sentry is not started only logs`() {
        logError("CrashReportingTest", "nothing to report to", IllegalStateException("boom"))
    }

    private companion object {
        const val DSN = "https://public@o0.ingest.sentry.io/0"
        const val MAPPING_ID = "c0ffee00-0000-4000-8000-000000000001"
    }
}
