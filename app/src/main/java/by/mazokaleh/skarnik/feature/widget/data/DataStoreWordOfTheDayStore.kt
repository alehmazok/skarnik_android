package by.mazokaleh.skarnik.feature.widget.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import by.mazokaleh.skarnik.feature.widget.domain.WordOfTheDay
import by.mazokaleh.skarnik.feature.widget.domain.WordOfTheDayStore
import javax.inject.Inject
import kotlinx.coroutines.flow.first

/** The word every «Слова дня» widget shows: all widgets show the same word, as in Flutter. */
class DataStoreWordOfTheDayStore @Inject constructor(
    @WidgetPreferences private val preferences: DataStore<Preferences>
) : WordOfTheDayStore {
    override suspend fun current(): WordOfTheDay? {
        val prefs = preferences.data.first()
        val wordId = prefs[WORD_ID]
        val word = prefs[WORD]
        return if (wordId != null && word != null) WordOfTheDay(wordId, word, prefs[TRANSLATION].orEmpty()) else null
    }

    override suspend fun save(word: WordOfTheDay) {
        preferences.edit { prefs ->
            prefs[WORD_ID] = word.wordId
            prefs[WORD] = word.word
            prefs[TRANSLATION] = word.translation
        }
    }

    companion object {
        val WORD_ID = longPreferencesKey("word_of_the_day_id")
        val WORD = stringPreferencesKey("word_of_the_day_word")
        val TRANSLATION = stringPreferencesKey("word_of_the_day_translation")

        fun withWord(prefs: Preferences, word: WordOfTheDay): Preferences = prefs.toMutablePreferences().apply {
            this[WORD_ID] = word.wordId
            this[WORD] = word.word
            this[TRANSLATION] = word.translation
        }
    }
}
