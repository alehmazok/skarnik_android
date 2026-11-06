package by.mazokaleh.skarnik.feature.whatsnew.data

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class DataStoreWhatsNewSettingsTest {
    @get:Rule
    val tmp = TemporaryFolder()

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    @After
    fun tearDown() {
        scope.cancel()
    }

    @Test
    fun `stores the last seen version`() = runBlocking {
        val store = PreferenceDataStoreFactory.create(
            scope = scope,
            produceFile = { File(tmp.root, "datastore/settings.preferences_pb") }
        )
        val settings = DataStoreWhatsNewSettings(store)

        assertNull(settings.lastSeenVersion())
        settings.setLastSeenVersion("4.0.0")
        assertEquals("4.0.0", settings.lastSeenVersion())
    }
}
