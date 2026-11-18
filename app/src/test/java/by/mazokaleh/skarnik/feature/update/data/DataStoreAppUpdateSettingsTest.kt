package by.mazokaleh.skarnik.feature.update.data

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

class DataStoreAppUpdateSettingsTest {
    @get:Rule
    val tmp = TemporaryFolder()

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    @After
    fun tearDown() {
        scope.cancel()
    }

    @Test
    fun `stores the prompted version`() = runBlocking {
        val store = PreferenceDataStoreFactory.create(
            scope = scope,
            produceFile = { File(tmp.root, "datastore/settings.preferences_pb") }
        )
        val settings = DataStoreAppUpdateSettings(store)

        assertNull(settings.promptedVersionCode())
        settings.setPromptedVersionCode(180)
        assertEquals(180L, settings.promptedVersionCode())
    }
}
