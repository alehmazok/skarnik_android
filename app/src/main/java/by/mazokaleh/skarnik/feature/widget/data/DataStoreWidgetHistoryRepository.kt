package by.mazokaleh.skarnik.feature.widget.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import by.mazokaleh.skarnik.core.common.WallClock
import by.mazokaleh.skarnik.feature.widget.domain.RefreshWordOfTheDayUseCase
import by.mazokaleh.skarnik.feature.widget.domain.WidgetHistoryEntry
import by.mazokaleh.skarnik.feature.widget.domain.WidgetHistoryRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.first
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json

/**
 * Flutter's `SharedPreferencesWidgetHistoryRepository`, as one JSON map `wordId → entry` in the widget's
 * DataStore. Flutter kept every entry forever; entries that no longer change a pick (not similar and older than
 * the reuse period, so the word is free again) are dropped on save, which picks the same words.
 */
class DataStoreWidgetHistoryRepository @Inject constructor(
    @WidgetPreferences private val preferences: DataStore<Preferences>,
    private val clock: WallClock
) : WidgetHistoryRepository {
    override suspend fun entry(wordId: Long): WidgetHistoryEntry? =
        decodeHistory(preferences.data.first()[HISTORY])[wordId.toString()]?.toEntry()

    override suspend fun save(wordId: Long, entry: WidgetHistoryEntry) {
        val now = clock.nowMillis()
        preferences.edit { prefs ->
            val history = decodeHistory(prefs[HISTORY])
                .filterValues { it.similar || now - it.shownAt < RefreshWordOfTheDayUseCase.REUSE_AFTER_MILLIS }
                .toMutableMap()
            history[wordId.toString()] = StoredEntry(entry.shownAtMillis, entry.similar)
            prefs[HISTORY] = encodeHistory(history)
        }
    }

    @Serializable
    data class StoredEntry(val shownAt: Long, val similar: Boolean) {
        fun toEntry() = WidgetHistoryEntry(shownAt, similar)
    }

    companion object {
        val HISTORY = stringPreferencesKey("widget_word_history")

        private val json = Json { ignoreUnknownKeys = true }

        private val serializer = MapSerializer(String.serializer(), StoredEntry.serializer())

        fun encodeHistory(history: Map<String, StoredEntry>): String = json.encodeToString(serializer, history)

        /** Bad JSON counts as an empty history: the widget may show a word again, nothing worse. */
        fun decodeHistory(value: String?): Map<String, StoredEntry> {
            if (value.isNullOrEmpty()) return emptyMap()
            return try {
                json.decodeFromString(serializer, value)
            } catch (ignored: IllegalArgumentException) {
                emptyMap()
            }
        }
    }
}
