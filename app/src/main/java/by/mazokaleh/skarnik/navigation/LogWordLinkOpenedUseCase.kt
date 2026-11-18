package by.mazokaleh.skarnik.navigation

import by.mazokaleh.skarnik.core.analytics.Analytics
import by.mazokaleh.skarnik.core.analytics.AnalyticsEvent.WidgetDeepLink
import by.mazokaleh.skarnik.core.common.suspendRunCatching
import by.mazokaleh.skarnik.core.crash.logError
import by.mazokaleh.skarnik.core.model.WordLink
import by.mazokaleh.skarnik.feature.translation.domain.WordRepository
import javax.inject.Inject

/**
 * Logs `widget_deep_link` for a word opened from a widget or an app link, with the word's text from the bundled
 * index (skarnik_ios looks it up the same way). The lookup never blocks opening the word.
 */
class LogWordLinkOpenedUseCase @Inject constructor(
    private val words: WordRepository,
    private val analytics: Analytics
) {
    suspend operator fun invoke(link: WordLink, appState: WidgetDeepLink.AppState, source: WidgetDeepLink.Source) {
        val word = suspendRunCatching { words.find(link.dictionary, link.wordId)?.word }
            .onFailure { logError(TAG, "Cannot look up the opened word", it) }
            .getOrNull()
        analytics.log(WidgetDeepLink(link, word.orEmpty(), appState, source))
    }

    private companion object {
        const val TAG = "WordLinkOpened"
    }
}
