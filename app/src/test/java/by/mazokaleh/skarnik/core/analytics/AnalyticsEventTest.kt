package by.mazokaleh.skarnik.core.analytics

import by.mazokaleh.skarnik.BuildConfig
import by.mazokaleh.skarnik.core.model.Dictionary
import by.mazokaleh.skarnik.core.model.Word
import by.mazokaleh.skarnik.core.model.WordLink
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

    @Test
    fun `word link events`() {
        val event = AnalyticsEvent.TranslationWordClicked(word, "янтар", WordLink(Dictionary.BelRus, 7))

        assertEquals("translation_word_clicked", event.name)
        assertEquals(
            mapOf(
                "word" to "янтар",
                "dict_name" to word.dictionary.analyticsName,
                "dict_path" to word.dictionary.path,
                "target_dict_path" to "belrus",
                "target_word_id" to 7L
            ),
            event.params
        )
    }

    @Test
    fun `dictionary link event`() {
        val event = AnalyticsEvent.DictionaryLinkClicked(word, WordLink(Dictionary.Tsbm, 10976), "footer")

        assertEquals("dictionary_link_clicked", event.name)
        assertEquals(
            mapOf(
                "word" to word.word,
                "dict_name" to word.dictionary.analyticsName,
                "dict_path" to word.dictionary.path,
                "target_dict_path" to "tsbm",
                "target_word_id" to 10976L,
                "placement" to "footer"
            ),
            event.params
        )
    }

    @Test
    fun `widget deep link has skarnik_ios's parameters plus source`() {
        val event = AnalyticsEvent.WidgetDeepLink(
            WordLink(Dictionary.BelRus, 27890),
            "бурштын",
            AnalyticsEvent.WidgetDeepLink.AppState.ColdStart,
            AnalyticsEvent.WidgetDeepLink.Source.Widget
        )

        assertEquals("widget_deep_link", event.name)
        assertEquals(
            mapOf(
                "word" to "бурштын",
                "word_id" to 27890L,
                "lang_id" to 1L,
                "app_state" to "cold_start",
                "source" to "widget"
            ),
            event.params
        )
        assertEquals(
            "background",
            AnalyticsEvent.WidgetDeepLink.AppState.Background.analyticsName
        )
        assertEquals("app_link", AnalyticsEvent.WidgetDeepLink.Source.AppLink.analyticsName)
    }

    @Test
    fun `screen_view has Firebase's recommended parameters`() {
        val event = AnalyticsEvent.ScreenView(Screen.Favorites)

        assertEquals("screen_view", event.name)
        assertEquals(mapOf("screen_name" to "favorites", "screen_class" to "HomeRoute"), event.params)
    }

    @Test
    fun `screen names are stable and unique`() {
        assertEquals(
            listOf(
                "history",
                "favorites",
                "vocabulary",
                "search",
                "translation",
                "stress",
                "stress_table",
                "settings"
            ),
            Screen.entries.map { it.screenName }
        )
    }
}
