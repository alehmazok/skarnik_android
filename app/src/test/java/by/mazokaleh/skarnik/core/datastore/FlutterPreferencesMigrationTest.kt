package by.mazokaleh.skarnik.core.datastore

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import by.mazokaleh.skarnik.feature.favorites.data.DataStoreBookmarksSortRepository
import by.mazokaleh.skarnik.feature.favorites.data.bookmarksSortMigration
import by.mazokaleh.skarnik.feature.favorites.domain.BookmarksSortOrder
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

/** Moving Flutter's settings, on real DataStore files laid out as on the device. */
class FlutterPreferencesMigrationTest {
    @get:Rule
    val tmp = TemporaryFolder()

    private val scopes = mutableListOf<CoroutineScope>()

    @After
    fun tearDown() {
        scopes.forEach { it.cancel() }
    }

    /** DataStore allows one instance per file, so each "launch" gets a fresh scope after the previous closes. */
    private fun store(name: String, flutter: DataStore<Preferences>? = null): DataStore<Preferences> {
        val scope = CoroutineScope(Dispatchers.IO + SupervisorJob()).also { scopes += it }
        return PreferenceDataStoreFactory.create(
            migrations = listOfNotNull(flutter?.let(::bookmarksSortMigration)),
            scope = scope,
            produceFile = { File(tmp.root, "datastore/$name.preferences_pb") }
        )
    }

    @Test
    fun `the bookmarks sort order moves over from Flutter`() = runBlocking {
        val flutter = store("FlutterSharedPreferences")
        flutter.edit { it[stringPreferencesKey("favorites_sort_order")] = "alphabetical" }

        val repository = DataStoreBookmarksSortRepository(store("settings", flutter))

        assertEquals(BookmarksSortOrder.Alphabetical, repository.sortOrder.first())
    }

    @Test
    fun `no Flutter value means the default order`() = runBlocking {
        val repository = DataStoreBookmarksSortRepository(store("settings", store("FlutterSharedPreferences")))

        assertEquals(BookmarksSortOrder.DateAdded, repository.sortOrder.first())
    }

    @Test
    fun `the migration runs once, so a later change is not overwritten`() = runBlocking {
        val flutter = store("FlutterSharedPreferences")
        flutter.edit { it[stringPreferencesKey("favorites_sort_order")] = "alphabetical" }
        val settings = store("settings", flutter)
        DataStoreBookmarksSortRepository(settings).setSortOrder(BookmarksSortOrder.DateAdded)

        val migration = bookmarksSortMigration(flutter)
        val current = settings.data.first()

        assertEquals(false, migration.shouldMigrate(current))
        assertEquals(BookmarksSortOrder.DateAdded, DataStoreBookmarksSortRepository(settings).sortOrder.first())
    }
}
