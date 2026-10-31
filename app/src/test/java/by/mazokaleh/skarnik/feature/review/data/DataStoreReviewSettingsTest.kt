package by.mazokaleh.skarnik.feature.review.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
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

class DataStoreReviewSettingsTest {
    @get:Rule
    val tmp = TemporaryFolder()

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    @After
    fun tearDown() {
        scope.cancel()
    }

    private fun dataStore(name: String, flutter: DataStore<Preferences>? = null) = PreferenceDataStoreFactory.create(
        migrations = listOfNotNull(flutter?.let(::reviewMigration)),
        scope = scope,
        produceFile = { File(tmp.root, "datastore/$name.preferences_pb") }
    )

    @Test
    fun `counts views and remembers the request under Flutter's keys`() = runBlocking {
        val store = dataStore("settings")
        val settings = DataStoreReviewSettings(store)

        assertFalse(settings.isReviewRequested())
        assertEquals(1, settings.incrementTranslationViewCount())
        assertEquals(2, settings.incrementTranslationViewCount())
        settings.markReviewRequested()

        assertTrue(settings.isReviewRequested())
        val prefs = store.data.first()
        assertEquals(2L, prefs[longPreferencesKey("review_translation_view_count")])
        assertEquals(true, prefs[booleanPreferencesKey("review_already_requested")])
    }

    @Test
    fun `migrates Flutter's count and flag`() = runBlocking {
        val flutter = dataStore("FlutterSharedPreferences")
        flutter.edit {
            it[longPreferencesKey("review_translation_view_count")] = 7L
            it[booleanPreferencesKey("review_already_requested")] = true
        }
        val settings = DataStoreReviewSettings(dataStore("settings", flutter))

        assertTrue(settings.isReviewRequested())
        assertEquals(8, settings.incrementTranslationViewCount())
    }

    @Test
    fun `migrates an int count and skips values of another type`() = runBlocking {
        val flutter = dataStore("FlutterSharedPreferences")
        flutter.edit {
            it[intPreferencesKey("review_translation_view_count")] = 4
            it[stringPreferencesKey("review_already_requested")] = "yes"
        }
        val store = dataStore("settings", flutter)

        assertNull(store.data.first()[DataStoreReviewSettings.REQUESTED])
        assertEquals(5, DataStoreReviewSettings(store).incrementTranslationViewCount())
    }
}
