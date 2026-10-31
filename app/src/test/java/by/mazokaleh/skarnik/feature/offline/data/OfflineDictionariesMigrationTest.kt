package by.mazokaleh.skarnik.feature.offline.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import by.mazokaleh.skarnik.core.model.Dictionary
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

/** Download cursors and the promo flag move from Flutter's DataStore; the settings work on a real file. */
class OfflineDictionariesMigrationTest {
    @get:Rule
    val tmp = TemporaryFolder()

    private val scopes = mutableListOf<CoroutineScope>()

    @After
    fun tearDown() {
        scopes.forEach { it.cancel() }
    }

    private fun store(name: String, flutter: DataStore<Preferences>? = null): DataStore<Preferences> {
        val scope = CoroutineScope(Dispatchers.IO + SupervisorJob()).also { scopes += it }
        return PreferenceDataStoreFactory.create(
            migrations = listOfNotNull(flutter?.let(::offlineDictionariesMigration)),
            scope = scope,
            produceFile = { File(tmp.root, "datastore/$name.preferences_pb") }
        )
    }

    @Test
    fun `Flutter's cursors and promo flag move over`() = runBlocking {
        // shared_preferences_android stores a Dart int as a Long key.
        val flutter = store("FlutterSharedPreferences")
        flutter.edit {
            it[longPreferencesKey("download_cursor_belrus")] = 45_000L
            it[booleanPreferencesKey("offline_dictionaries_promo_seen")] = true
        }

        val settings = DataStoreOfflineDictionarySettings(store("settings", flutter))

        assertEquals(45_000L, settings.cursor(Dictionary.BelRus).first())
        assertNull(settings.cursor(Dictionary.Tsbm).first())
        assertTrue(settings.promoSeen.first())
    }

    @Test
    fun `a value of an unexpected type is skipped`() = runBlocking {
        val flutter = store("FlutterSharedPreferences")
        flutter.edit { it[stringPreferencesKey("download_cursor_tsbm")] = "oops" }

        val settings = DataStoreOfflineDictionarySettings(store("settings", flutter))

        assertNull(settings.cursor(Dictionary.Tsbm).first())
        assertFalse(settings.promoSeen.first())
    }

    @Test
    fun `cursors, attempts and the promo flag are saved`() = runBlocking {
        val settings = DataStoreOfflineDictionarySettings(store("settings", store("FlutterSharedPreferences")))

        settings.setCursor(Dictionary.RusBel, 7)
        settings.setRecentDownloadAttempts(listOf(1L, 2L))
        settings.markPromoSeen()
        assertEquals(7L, settings.cursor(Dictionary.RusBel).first())
        assertEquals(setOf(1L, 2L), settings.recentDownloadAttempts().toSet())
        assertTrue(settings.promoSeen.first())

        settings.clearCursor(Dictionary.RusBel)
        assertNull(settings.cursor(Dictionary.RusBel).first())
    }
}
