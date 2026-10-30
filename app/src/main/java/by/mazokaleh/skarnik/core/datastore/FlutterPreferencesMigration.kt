package by.mazokaleh.skarnik.core.datastore

import android.util.Log
import androidx.datastore.core.DataMigration
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.emptyPreferences
import by.mazokaleh.skarnik.core.common.suspendRunCatching
import kotlinx.coroutines.flow.first

/**
 * Moves one feature's keys from Flutter's preferences into the app's store, once per install. [name] identifies
 * the feature (it becomes the "done" flag). Flutter's file is left as it is.
 *
 * Unreadable Flutter preferences count as empty: the feature then starts from its defaults, and the app's own
 * store still opens.
 */
class FlutterPreferencesMigration(
    private val flutterPreferences: DataStore<Preferences>,
    name: String,
    private val copy: (from: Preferences, to: MutablePreferences) -> Unit
) : DataMigration<Preferences> {
    private val doneKey = booleanPreferencesKey("migrated_from_flutter_$name")

    override suspend fun shouldMigrate(currentData: Preferences): Boolean = currentData[doneKey] != true

    override suspend fun migrate(currentData: Preferences): Preferences {
        val flutter = suspendRunCatching { flutterPreferences.data.first() }.getOrElse {
            Log.w(TAG, "Cannot read Flutter preferences", it)
            emptyPreferences()
        }
        return currentData.toMutablePreferences().apply {
            copy(flutter, this)
            this[doneKey] = true
        }
    }

    override suspend fun cleanUp() = Unit

    private companion object {
        const val TAG = "FlutterPreferences"
    }
}
