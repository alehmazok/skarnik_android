package by.mazokaleh.skarnik.core.analytics

import by.mazokaleh.skarnik.BuildConfig
import by.mazokaleh.skarnik.core.model.Dictionary
import by.mazokaleh.skarnik.core.model.Word
import by.mazokaleh.skarnik.feature.search.domain.SearchWord
import by.mazokaleh.skarnik.feature.translation.domain.Translation

/**
 * An analytics event with Flutter's name and parameters, so the Firebase reports continue across the update.
 * Parameter values are [String] or [Long].
 */
sealed class AnalyticsEvent(val name: String) {
    abstract val params: Map<String, Any>

    /** Flutter: `logAppOpen()` once the app has started. */
    data object AppOpen : AnalyticsEvent("app_open") {
        override val params: Map<String, Any> = emptyMap()
    }

    /** Logged when the results of [query] stayed on screen for a moment (see SearchViewModel). */
    data class SearchPerformed(val query: String, val resultCount: Int, val usedPrepositionFallback: Boolean) :
        AnalyticsEvent("search_performed") {
        override val params: Map<String, Any> = mapOf(
            "query" to query,
            "result_count" to resultCount.toLong(),
            "used_preposition_fallback" to usedPrepositionFallback.toLong()
        )
    }

    data class SearchNoResults(val query: String, val usedPrepositionFallback: Boolean) :
        AnalyticsEvent("search_no_results") {
        override val params: Map<String, Any> = mapOf(
            "query" to query,
            "used_preposition_fallback" to usedPrepositionFallback.toLong()
        )
    }

    /** [position] is the row's index in the results, from 0. */
    data class SearchResultTapped(val word: SearchWord, val position: Int, val query: String) :
        AnalyticsEvent("search_result_tapped") {
        override val params: Map<String, Any> = wordParams(word.dictionary, word.wordId, word.word) +
            mapOf("position" to position.toLong(), "query" to query)
    }

    data class TranslationViewed(val translation: Translation) : AnalyticsEvent("translation") {
        override val params: Map<String, Any> = with(translation.word) {
            mapOf("uri" to apiUrl(dictionary, wordId)) +
                wordParams(dictionary, wordId, word) +
                mapOf("source" to translation.source.analyticsName)
        }
    }

    /** Firebase's recommended `share` event, as Flutter's `logShare`. [link] is the shared skarnik.app link. */
    data class Share(val link: String) : AnalyticsEvent("share") {
        override val params: Map<String, Any> =
            mapOf("content_type" to "text/plain", "item_id" to link, "method" to "system")
    }

    data class AddToFavorites(val word: Word) : AnalyticsEvent("add_to_favorites") {
        override val params: Map<String, Any> = wordParams(word.dictionary, word.wordId, word.word)
    }

    /** A word opened from the Dictionary tab. */
    data class VocabularyWordOpened(val dictionary: Dictionary, val wordId: Long, val word: String) :
        AnalyticsEvent("vocabulary") {
        override val params: Map<String, Any> = wordParams(dictionary, wordId, word)
    }

    data class OfflineDictionaryDownloadClicked(val dictionary: Dictionary) :
        AnalyticsEvent("offline_dictionary_download_click") {
        override val params: Map<String, Any> =
            mapOf("dict_name" to dictionary.analyticsName, "dict_path" to dictionary.path)
    }

    /** The «На́ціск» screen opened for [word]. */
    data class StressClicked(val word: String) : AnalyticsEvent("stress_clicked") {
        override val params: Map<String, Any> = mapOf("word" to word)
    }
}

/** Flutter's `Dictionary` enum names, which the existing reports group by. */
val Dictionary.analyticsName: String
    get() = when (this) {
        Dictionary.BelRus -> "belRus"
        Dictionary.RusBel -> "rusBel"
        Dictionary.Tsbm -> "tsbm"
    }

private val Translation.Source.analyticsName: String
    get() = when (this) {
        Translation.Source.Local -> "local"
        Translation.Source.Api -> "api"
        Translation.Source.Cloud -> "cloud"
    }

private fun Boolean.toLong(): Long = if (this) 1L else 0L

private fun wordParams(dictionary: Dictionary, wordId: Long, word: String): Map<String, Any> = mapOf(
    "word_id" to wordId,
    "lang_id" to dictionary.langId,
    "word" to word,
    "dict_name" to dictionary.analyticsName,
    "dict_path" to dictionary.path
)

/** Flutter logged the admin API address of the word whatever source answered. */
private fun apiUrl(dictionary: Dictionary, wordId: Long): String =
    "${BuildConfig.API_BASE_URL.trimEnd('/')}/api/words/${dictionary.path}/$wordId/"
