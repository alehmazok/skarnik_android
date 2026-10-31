package by.mazokaleh.skarnik.core.analytics

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.edit
import java.io.File
import kotlin.test.assertEquals
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class DataStoreAnalyticsConsentTest {
    @get:Rule
    val tmp = TemporaryFolder()

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val analytics = FakeAnalytics()

    @After
    fun tearDown() {
        scope.cancel()
    }

    private fun dataStore(name: String) = PreferenceDataStoreFactory.create(
        scope = scope,
        produceFile = { File(tmp.root, "datastore/$name.preferences_pb") }
    )

    @Test
    fun `nothing is collected before an answer`() = runBlocking {
        val consent = DataStoreAnalyticsConsent(dataStore("settings"), analytics)

        assertEquals(ConsentState(answered = false, granted = false), consent.state.first())
        consent.applyStored()
        assertEquals(listOf(false), analytics.collectionEnabled)
    }

    @Test
    fun `an answer is saved under Flutter's keys and applied`() = runBlocking {
        val settings = dataStore("settings")
        val consent = DataStoreAnalyticsConsent(settings, analytics)

        consent.setGranted(true)
        assertEquals(ConsentState(answered = true, granted = true), consent.state.first())
        consent.setGranted(false)

        assertEquals(ConsentState(answered = true, granted = false), consent.state.first())
        assertEquals(true, settings.data.first()[DataStoreAnalyticsConsent.ANSWERED])
        assertEquals(listOf(true, false), analytics.collectionEnabled)
    }

    @Test
    fun `the stored answer is applied at start`() = runBlocking {
        val settings = dataStore("settings")
        DataStoreAnalyticsConsent(settings, FakeAnalytics()).setGranted(true)

        DataStoreAnalyticsConsent(settings, analytics).applyStored()

        assertEquals(listOf(true), analytics.collectionEnabled)
    }

    @Test
    fun `the Flutter answer is migrated, so the dialog is not shown again`() = runBlocking {
        val flutter = dataStore("FlutterSharedPreferences")
        flutter.edit {
            it[DataStoreAnalyticsConsent.ANSWERED] = true
            it[DataStoreAnalyticsConsent.GRANTED] = true
        }
        val settings = PreferenceDataStoreFactory.create(
            migrations = listOf(analyticsConsentMigration(flutter)),
            scope = scope,
            produceFile = { File(tmp.root, "datastore/migrated.preferences_pb") }
        )

        val consent = DataStoreAnalyticsConsent(settings, analytics)

        assertEquals(ConsentState(answered = true, granted = true), consent.state.first())
    }
}
