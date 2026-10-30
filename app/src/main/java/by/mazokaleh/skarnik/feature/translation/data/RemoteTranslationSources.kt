package by.mazokaleh.skarnik.feature.translation.data

import by.mazokaleh.skarnik.core.model.Dictionary
import by.mazokaleh.skarnik.core.network.fetchString
import by.mazokaleh.skarnik.feature.translation.domain.RemoteTranslationSource
import by.mazokaleh.skarnik.feature.translation.domain.TranslationEntry
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request

/** The skarnik_admin row shape, shared by the admin API and Supabase `main_word`. */
@Serializable
internal data class WordDto(
    val stress: String? = null,
    val translation: String? = null,
    @SerialName("redirect_to") val redirectTo: String? = null
) {
    fun toEntry() = TranslationEntry(stress = stress, html = translation, redirectTo = redirectTo)
}

/** `GET <base>/api/words/<dict>/<id>/` on skarnik_admin. */
class AdminApiTranslationSource(
    private val client: OkHttpClient,
    private val json: Json,
    private val baseUrl: HttpUrl
) : RemoteTranslationSource {
    override suspend fun fetch(dictionary: Dictionary, wordId: Long): TranslationEntry {
        val url = baseUrl.newBuilder()
            .addPathSegments("api/words/${dictionary.path}/$wordId/")
            .build()
        val body = client.fetchString(Request.Builder().url(url).build())
        return json.decodeFromString<WordDto>(body).toEntry()
    }
}

/** Supabase PostgREST on `main_word`, filtered by `external_id` and `direction` (the id alone is not unique). */
class SupabaseTranslationSource(
    private val client: OkHttpClient,
    private val json: Json,
    private val projectUrl: HttpUrl,
    private val apiKey: String
) : RemoteTranslationSource {
    override suspend fun fetch(dictionary: Dictionary, wordId: Long): TranslationEntry {
        val url = projectUrl.newBuilder()
            .addPathSegments("rest/v1/main_word")
            .addQueryParameter("select", "stress,translation,redirect_to")
            .addQueryParameter("external_id", "eq.$wordId")
            .addQueryParameter("direction", "eq.${dictionary.path}")
            .addQueryParameter("limit", "1")
            .build()
        val request = Request.Builder()
            .url(url)
            .header("apikey", apiKey)
            .header("Authorization", "Bearer $apiKey")
            .build()
        val rows = json.decodeFromString<List<WordDto>>(client.fetchString(request))
        val row = rows.firstOrNull() ?: throw NoSuchElementException("No ${dictionary.path}/$wordId in Supabase")
        return row.toEntry()
    }
}

internal fun String.toBaseUrl(): HttpUrl = trimEnd('/').toHttpUrl()
