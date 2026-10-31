package by.mazokaleh.skarnik.feature.widget.domain

import android.util.Log
import by.mazokaleh.skarnik.core.common.WallClock
import by.mazokaleh.skarnik.core.common.suspendRunCatching
import by.mazokaleh.skarnik.core.model.Word
import by.mazokaleh.skarnik.feature.translation.domain.GetTranslationUseCase
import by.mazokaleh.skarnik.feature.translation.domain.Translation
import by.mazokaleh.skarnik.feature.translation.domain.TranslationResult
import javax.inject.Inject

/** When a word was last picked for the widget, and whether its translation was too close to it. */
data class WidgetHistoryEntry(val shownAtMillis: Long, val similar: Boolean)

/** The widget's own record of picked words, keyed by word id. Separate from the app's history. */
interface WidgetHistoryRepository {
    suspend fun entry(wordId: Long): WidgetHistoryEntry?

    suspend fun save(wordId: Long, entry: WidgetHistoryEntry)
}

/**
 * Flutter's `RefreshWidgetDataUseCase`: pick a random Бел-Рус word, skip it if it was picked in the last 30 days
 * (or was ever too close to its translation), get its translation, and reject it when the translation is
 * basically the same word. Up to [MAX_ATTEMPTS] picks.
 *
 * Returns null when no word was found. Flutter showed the fallback word then; the widget keeps the word it
 * already shows instead, so a refresh without network does not replace a good word.
 */
class RefreshWordOfTheDayUseCase @Inject constructor(
    private val words: RandomWordSource,
    private val history: WidgetHistoryRepository,
    private val getTranslation: GetTranslationUseCase,
    private val clock: WallClock
) {
    suspend operator fun invoke(): WordOfTheDay? {
        var picked: WordOfTheDay? = null
        var attempts = 0
        while (picked == null && attempts < MAX_ATTEMPTS) {
            attempts++
            val word = words.randomBelRusWord() ?: break
            picked = attempt(word)
        }
        if (picked == null) Log.w(TAG, "No word after $attempts attempts")
        return picked
    }

    /** The word to show, or null when [word] is skipped. */
    private suspend fun attempt(word: Word): WordOfTheDay? {
        val seen = history.entry(word.wordId)
        val translation = if (seen == null || seen.isReusable()) translate(word) else null
        if (translation == null) return null
        val similar = WordSimilarity.isSimilar(word.word, translation.html)
        history.save(word.wordId, WidgetHistoryEntry(clock.nowMillis(), similar))
        val plainText = WordSimilarity.htmlToPlainText(translation.html)
        return if (similar) null else WordOfTheDay(word.wordId, word.word, plainText)
    }

    /** Null when no source has the word. A redirect has no translation of its own to show. */
    private suspend fun translate(word: Word): Translation? {
        val result = suspendRunCatching { getTranslation(word) }
            .onFailure { Log.w(TAG, "No translation for ${word.wordId}", it) }
            .getOrNull()
        return (result as? TranslationResult.Found)?.translation
    }

    /** Picked within the reuse period: skip. Earlier: reusable unless it was too close to its translation. */
    private fun WidgetHistoryEntry.isReusable(): Boolean =
        clock.nowMillis() - shownAtMillis >= REUSE_AFTER_MILLIS && !similar

    companion object {
        const val MAX_ATTEMPTS = 30
        const val REUSE_AFTER_MILLIS = 30L * 24 * 60 * 60 * 1000
        private const val TAG = "WordOfTheDay"
    }
}
