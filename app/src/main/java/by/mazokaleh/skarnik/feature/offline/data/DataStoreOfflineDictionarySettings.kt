package by.mazokaleh.skarnik.feature.offline.data

import androidx.datastore.core.DataMigration
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import by.mazokaleh.skarnik.core.datastore.FlutterPreferencesMigration
import by.mazokaleh.skarnik.core.model.Dictionary
import by.mazokaleh.skarnik.feature.offline.domain.OfflineDictionarySettings
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

/** Flutter's download cursor, and rate limit repositories, with the same key names. */
class DataStoreOfflineDictionarySettings @Inject constructor(private val settings: DataStore<Preferences>) :
    OfflineDictionarySettings {
    override fun cursor(dictionary: Dictionary): Flow<Long?> = settings.data
        .map { it[cursorKey(dictionary)] }
        .distinctUntilChanged()

    override suspend fun setCursor(dictionary: Dictionary, rowId: Long) {
        settings.edit { it[cursorKey(dictionary)] = rowId }
    }

    override suspend fun clearCursor(dictionary: Dictionary) {
        settings.edit { it.remove(cursorKey(dictionary)) }
    }

    override suspend fun recentDownloadAttempts(): List<Long> =
        settings.data.first()[RECENT_ATTEMPTS].orEmpty().mapNotNull { it.toLongOrNull() }

    override suspend fun setRecentDownloadAttempts(attempts: List<Long>) {
        settings.edit { prefs -> prefs[RECENT_ATTEMPTS] = attempts.mapTo(mutableSetOf()) { it.toString() } }
    }

    companion object {
        fun cursorKey(dictionary: Dictionary) = longPreferencesKey("download_cursor_${dictionary.path}")

        /**
         * Not migrated from Flutter: it only matters for five minutes, and Flutter stored it as an encoded
         * string list.
         */
        val RECENT_ATTEMPTS = stringSetPreferencesKey("download_recent_attempts_at_ms")
    }
}

/**
 * Copies Flutter's download cursors, so a half-finished download resumes. Values are read by name and checked
 * by type, so a value Flutter stored with another type is skipped instead of crashing. Flutter's promo flag
 * (`offline_dictionaries_promo_seen`) is not copied: the settings badge it drove was dropped.
 */
fun offlineDictionariesMigration(flutterPreferences: DataStore<Preferences>): DataMigration<Preferences> =
    FlutterPreferencesMigration(flutterPreferences, name = "offline_dictionaries") { from, to ->
        val values = from.asMap().mapKeys { it.key.name }
        Dictionary.entries.forEach { dictionary ->
            val key = DataStoreOfflineDictionarySettings.cursorKey(dictionary)
            when (val cursor = values[key.name]) {
                is Long -> to[key] = cursor
                is Int -> to[key] = cursor.toLong()
            }
        }
    }
