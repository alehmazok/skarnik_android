package by.mazokaleh.skarnik.core.analytics

import by.mazokaleh.skarnik.BuildConfig
import by.mazokaleh.skarnik.core.model.Dictionary
import by.mazokaleh.skarnik.core.model.Word
import by.mazokaleh.skarnik.feature.search.domain.SearchWord
import by.mazokaleh.skarnik.feature.translation.domain.Translation
import kotlin.test.assertEquals
import org.junit.Test

/** Names and parameters must match skarnik-flutter's, so the Firebase reports continue. */
class AnalyticsEventTest {
    private val word = Word(Dictionary.BelRus, 27890, "бурштын", "б", "бурштын", null)

    @Test
    fun `translation has Flutter's parameters`() {
        val event = AnalyticsEvent.TranslationViewed(Translation(word, null, "янтарь", Translation.Source.Api))

        assertEquals("translation", event.name)
        assertEquals(
            mapOf(
                "uri" to "${BuildConfig.API_BASE_URL.trimEnd('/')}/api/words/belrus/27890/",
                "word_id" to 27890L,
                "lang_id" to 1L,
                "word" to "бурштын",
                "dict_name" to "belRus",
                "dict_path" to "belrus",
                "source" to "api"
            ),
            event.params
        )
    }

    @Test
    fun `search events`() {
        assertEquals(
            mapOf("query" to "хата", "result_count" to 3L, "used_preposition_fallback" to 1L),
            AnalyticsEvent.SearchPerformed("хата", 3, usedPrepositionFallback = true).params
        )
        assertEquals(
            mapOf("query" to "ыыы", "used_preposition_fallback" to 0L),
            AnalyticsEvent.SearchNoResults("ыыы", usedPrepositionFallback = false).params
        )
        val searchWord = SearchWord(5, Dictionary.RusBel, 7, "дом", "д", "дом")
        val tapped = AnalyticsEvent.SearchResultTapped(searchWord, position = 2, query = "до")
        assertEquals("search_result_tapped", tapped.name)
        assertEquals(
            mapOf(
                "word_id" to 7L,
                "lang_id" to 0L,
                "word" to "дом",
                "dict_name" to "rusBel",
                "dict_path" to "rusbel",
                "position" to 2L,
                "query" to "до"
            ),
            tapped.params
        )
    }

    @Test
    fun `share is Firebase's recommended event`() {
        val event = AnalyticsEvent.Share("https://skarnik.app/r/belrus/27890")

        assertEquals("share", event.name)
        assertEquals(
            mapOf(
                "content_type" to "text/plain",
                "item_id" to "https://skarnik.app/r/belrus/27890",
                "method" to "system"
            ),
            event.params
        )
    }

    @Test
    fun `other events keep Flutter's names`() {
        assertEquals("app_open", AnalyticsEvent.AppOpen.name)
        assertEquals("add_to_favorites", AnalyticsEvent.AddToFavorites(word).name)
        assertEquals("vocabulary", AnalyticsEvent.VocabularyWordOpened(Dictionary.Tsbm, 1, "а").name)
        assertEquals("tsbm", AnalyticsEvent.VocabularyWordOpened(Dictionary.Tsbm, 1, "а").params["dict_name"])
        assertEquals(
            mapOf("dict_name" to "tsbm", "dict_path" to "tsbm"),
            AnalyticsEvent.OfflineDictionaryDownloadClicked(Dictionary.Tsbm).params
        )
        assertEquals(mapOf("word" to "пара"), AnalyticsEvent.StressClicked("пара").params)
    }
}
