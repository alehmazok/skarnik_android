package by.mazokaleh.skarnik.feature.widget.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import by.mazokaleh.skarnik.core.common.WallClock
import by.mazokaleh.skarnik.feature.widget.domain.WidgetHistoryEntry
import by.mazokaleh.skarnik.feature.widget.domain.WordOfTheDay
import java.io.File
import java.util.TimeZone
import kotlin.test.assertEquals
import kotlin.test.assertNull
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

/** The widget's history and current word on real DataStore files, and the move from Flutter's preferences. */
class WidgetDataStoreTest {
    @get:Rule
    val tmp = TemporaryFolder()

    private val scopes = mutableListOf<CoroutineScope>()
    private val minsk = TimeZone.getTimeZone("Europe/Minsk")
    private val day = 24L * 60 * 60 * 1000

    /** 2026-10-30T12:00:00 in Minsk (UTC+3). */
    private val noon = 1_793_350_800_000L

    @After
    fun tearDown() {
        scopes.forEach { it.cancel() }
    }

    private fun store(name: String, flutter: DataStore<Preferences>? = null): DataStore<Preferences> {
        val scope = CoroutineScope(Dispatchers.IO + SupervisorJob()).also { scopes += it }
        return PreferenceDataStoreFactory.create(
            migrations = listOfNotNull(flutter?.let { widgetHistoryMigration(it, minsk) }),
            scope = scope,
            produceFile = { File(tmp.root, "datastore/$name.preferences_pb") }
        )
    }

    @Test
    fun `Dart's local and UTC ISO dates are read`() {
        assertEquals(noon, parseDartDateTime("2026-10-30T12:00:00.123456", minsk))
        assertEquals(noon, parseDartDateTime("2026-10-30T12:00:00.000", minsk))
        assertEquals(noon, parseDartDateTime("2026-10-30T09:00:00.000Z", minsk))
        assertNull(parseDartDateTime("yesterday", minsk))
    }

    @Test
    fun `Flutter's widget history moves over, unreadable entries are skipped`() = runBlocking {
        val flutter = store("FlutterSharedPreferences")
        flutter.edit {
            it[stringPreferencesKey(FLUTTER_WIDGET_HISTORY)] =
                """{"27890":{"wordId":27890,"word":"бурштын","translation":"янтарь",""" +
                """"createdAt":"2026-10-30T12:00:00.000","isSimilar":false},""" +
                """"2":{"wordId":2,"word":"вада","translation":"вода","createdAt":"2026-10-01T12:00:00.000",""" +
                """"isSimilar":true},"3":{"createdAt":"bad","isSimilar":false},"x":{}}"""
        }
        val history = DataStoreWidgetHistoryRepository(store("word_of_the_day", flutter), WallClock { noon })

        assertEquals(WidgetHistoryEntry(noon, similar = false), history.entry(27890))
        assertEquals(WidgetHistoryEntry(noon - 29 * day, similar = true), history.entry(2))
        assertNull(history.entry(3))
    }

    @Test
    fun `no or broken Flutter history starts empty`() = runBlocking {
        val flutter = store("FlutterSharedPreferences")
        flutter.edit { it[stringPreferencesKey(FLUTTER_WIDGET_HISTORY)] = "not json" }

        assertNull(DataStoreWidgetHistoryRepository(store("word_of_the_day", flutter), WallClock { noon }).entry(1))
    }

    @Test
    fun `saving drops entries that no longer affect a pick`() = runBlocking {
        val preferences = store("word_of_the_day")
        var now = noon
        val history = DataStoreWidgetHistoryRepository(preferences, WallClock { now })
        history.save(1, WidgetHistoryEntry(noon, similar = false))
        history.save(2, WidgetHistoryEntry(noon, similar = true))
        history.save(3, WidgetHistoryEntry(noon + 5 * day, similar = false))

        now = noon + 31 * day
        history.save(4, WidgetHistoryEntry(now, similar = false))

        assertNull(history.entry(1))
        assertEquals(WidgetHistoryEntry(noon, similar = true), history.entry(2))
        assertEquals(WidgetHistoryEntry(noon + 5 * day, similar = false), history.entry(3))
        assertEquals(WidgetHistoryEntry(now, similar = false), history.entry(4))
        val stored = preferences.data.first()[DataStoreWidgetHistoryRepository.HISTORY]
        assertEquals(setOf("2", "3", "4"), DataStoreWidgetHistoryRepository.decodeHistory(stored).keys)
    }

    @Test
    fun `the current word is saved and read back`() = runBlocking {
        val words = DataStoreWordOfTheDayStore(store("word_of_the_day"))
        assertNull(words.current())

        words.save(WordOfTheDay(27890, "бурштын", "янтарь\nсмала"))

        assertEquals(WordOfTheDay(27890, "бурштын", "янтарь\nсмала"), words.current())
    }
}
