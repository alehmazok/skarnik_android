package by.mazokaleh.skarnik.navigation

import by.mazokaleh.skarnik.core.analytics.AnalyticsEvent.WidgetDeepLink
import by.mazokaleh.skarnik.core.analytics.FakeAnalytics
import by.mazokaleh.skarnik.core.model.Dictionary
import by.mazokaleh.skarnik.core.model.Word
import by.mazokaleh.skarnik.core.model.WordLink
import by.mazokaleh.skarnik.feature.translation.domain.WordRepository
import java.io.IOException
import kotlin.test.assertEquals
import kotlinx.coroutines.test.runTest
import org.junit.Test

class LogWordLinkOpenedUseCaseTest {
    private val analytics = FakeAnalytics()
    private val link = WordLink(Dictionary.BelRus, 27890)

    private fun useCase(find: (Dictionary, Long) -> Word?) = LogWordLinkOpenedUseCase(
        words = object : WordRepository {
            override suspend fun find(dictionary: Dictionary, wordId: Long) = find(dictionary, wordId)

            override suspend fun belarusianWords(keys: Set<String>): Map<String, WordLink> = emptyMap()

            override suspend fun sameWord(lword: String, dictionary: Dictionary): WordLink? = null
        },
        analytics = analytics
    )

    @Test
    fun `logs the word from the index`() = runTest {
        useCase { dictionary, id -> Word(dictionary, id, "бурштын", "б", "бурштын", null) }(
            link,
            WidgetDeepLink.AppState.ColdStart,
            WidgetDeepLink.Source.Widget
        )

        assertEquals(
            listOf(
                WidgetDeepLink(link, "бурштын", WidgetDeepLink.AppState.ColdStart, WidgetDeepLink.Source.Widget)
            ),
            analytics.events
        )
    }

    @Test
    fun `a word missing from the index is logged with an empty word`() = runTest {
        useCase { _, _ -> null }(link, WidgetDeepLink.AppState.Background, WidgetDeepLink.Source.AppLink)

        assertEquals(
            listOf(WidgetDeepLink(link, "", WidgetDeepLink.AppState.Background, WidgetDeepLink.Source.AppLink)),
            analytics.events
        )
    }

    @Test
    fun `a failed lookup still logs the event`() = runTest {
        useCase { _, _ -> throw IOException("store") }(
            link,
            WidgetDeepLink.AppState.Background,
            WidgetDeepLink.Source.Widget
        )

        assertEquals("", (analytics.events.single() as WidgetDeepLink).word)
    }
}
