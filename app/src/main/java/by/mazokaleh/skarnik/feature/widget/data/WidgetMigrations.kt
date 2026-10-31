package by.mazokaleh.skarnik.feature.widget.data

import android.content.Context
import androidx.datastore.core.DataMigration
import androidx.datastore.core.DataStore
import androidx.datastore.migrations.SharedPreferencesMigration
import androidx.datastore.preferences.core.Preferences
import by.mazokaleh.skarnik.core.datastore.FlutterPreferencesMigration
import by.mazokaleh.skarnik.feature.widget.data.DataStoreWidgetHistoryRepository.StoredEntry
import by.mazokaleh.skarnik.feature.widget.domain.WordOfTheDay
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull

/** Flutter's widget history key in `SharedPreferencesAsync`. */
const val FLUTTER_WIDGET_HISTORY = "widget_word_history_v1"

/**
 * Moves Flutter's widget history (`widget_word_history_v1`, a JSON string) into the widget's DataStore, so the
 * native widget does not repeat the words of the last 30 days. Entries it cannot read are skipped.
 */
fun widgetHistoryMigration(
    flutterPreferences: DataStore<Preferences>,
    timeZone: TimeZone = TimeZone.getDefault()
): DataMigration<Preferences> = FlutterPreferencesMigration(flutterPreferences, name = "widget_history") { from, to ->
    val value = from.asMap().entries.firstOrNull { it.key.name == FLUTTER_WIDGET_HISTORY }?.value as? String
    val history = parseFlutterHistory(value, timeZone)
    if (history.isNotEmpty()) {
        to[DataStoreWidgetHistoryRepository.HISTORY] = DataStoreWidgetHistoryRepository.encodeHistory(history)
    }
}

/** `{"<wordId>": {"createdAt": "2026-10-30T12:00:00.123456", "isSimilar": false, ...}}` → native entries. */
internal fun parseFlutterHistory(value: String?, timeZone: TimeZone): Map<String, StoredEntry> {
    val root = value?.takeIf { it.isNotEmpty() }?.let(::parseJsonObject) ?: return emptyMap()
    return root.mapNotNull { (wordId, element) ->
        val entry = (element as? JsonObject)?.takeIf { wordId.toLongOrNull() != null } ?: return@mapNotNull null
        val createdAt = (entry["createdAt"] as? JsonPrimitive)?.contentOrNull?.let { parseDartDateTime(it, timeZone) }
        val similar = (entry["isSimilar"] as? JsonPrimitive)?.booleanOrNull
        if (createdAt == null || similar == null) null else wordId to StoredEntry(createdAt, similar)
    }.toMap()
}

private fun parseJsonObject(value: String): JsonObject? = try {
    Json.parseToJsonElement(value) as? JsonObject
} catch (ignored: IllegalArgumentException) {
    null
}

/**
 * Dart's `DateTime.toIso8601String()`: local time without an offset, or UTC with a trailing `Z`. Fractions of a
 * second are ignored: the reuse period is 30 days. (`java.time` needs API 26; minSdk is 24.)
 */
internal fun parseDartDateTime(value: String, timeZone: TimeZone): Long? {
    val format = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.ROOT)
    format.timeZone = if (value.endsWith("Z")) TimeZone.getTimeZone("UTC") else timeZone
    format.isLenient = false
    return try {
        format.parse(value)?.time
    } catch (ignored: ParseException) {
        null
    }
}

/** The plugin Flutter's widget used (home_widget) keeps the shown word in these SharedPreferences. */
private const val HOME_WIDGET_PREFERENCES = "HomeWidgetPreferences"
private const val HOME_WIDGET_WORD = "word"
private const val HOME_WIDGET_TRANSLATION = "translation"
private const val HOME_WIDGET_WORD_ID = "wordId"

/**
 * Keeps the word the Flutter widget showed, so the widget does not change on the upgrade. home_widget stored
 * all three values as strings; the old keys are removed once copied.
 */
fun homeWidgetMigration(context: Context): DataMigration<Preferences> = SharedPreferencesMigration(
    context = context,
    sharedPreferencesName = HOME_WIDGET_PREFERENCES,
    keysToMigrate = setOf(HOME_WIDGET_WORD, HOME_WIDGET_TRANSLATION, HOME_WIDGET_WORD_ID)
) { legacy, current ->
    val word = legacy.getString(HOME_WIDGET_WORD, null)
    val translation = legacy.getString(HOME_WIDGET_TRANSLATION, null)
    val wordId = legacy.getString(HOME_WIDGET_WORD_ID, null)?.toLongOrNull()
    if (word == null || translation == null || wordId == null) {
        current
    } else {
        DataStoreWordOfTheDayStore.withWord(current, WordOfTheDay(wordId, word, translation))
    }
}
