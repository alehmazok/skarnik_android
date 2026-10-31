package by.mazokaleh.skarnik.feature.offline.data

import by.mazokaleh.skarnik.core.model.Dictionary
import by.mazokaleh.skarnik.core.network.fetch
import by.mazokaleh.skarnik.core.network.fetchString
import by.mazokaleh.skarnik.feature.offline.domain.DownloadedWord
import by.mazokaleh.skarnik.feature.offline.domain.OfflineDictionarySource
import java.io.IOException
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request

@Serializable
internal data class DownloadRowDto(
    val id: Long,
    @SerialName("external_id") val externalId: Long,
    val stress: String? = null,
    val translation: String? = null,
    @SerialName("redirect_to") val redirectTo: String? = null
) {
    fun toWord() = DownloadedWord(
        rowId = id,
        wordId = externalId,
        stress = stress,
        translation = translation.orEmpty(),
        redirectTo = redirectTo
    )
}

/** Supabase PostgREST on `main_word`: keyset pages by `id` within one `direction`, as Flutter downloads them. */
class SupabaseOfflineDictionarySource(
    private val client: OkHttpClient,
    private val json: Json,
    private val projectUrl: HttpUrl,
    private val apiKey: String
) : OfflineDictionarySource {
    override suspend fun countWords(dictionary: Dictionary): Long {
        val url = table()
            .addQueryParameter("select", "id")
            .addQueryParameter("direction", "eq.${dictionary.path}")
            .addQueryParameter("limit", "1")
            .build()
        // PostgREST answers `Content-Range: 0-0/<total>` when asked for an exact count.
        val request = request(url).newBuilder().header("Prefer", "count=exact").build()
        val range = client.fetch(request) { it.header("Content-Range") }
        return range?.substringAfterLast('/')?.toLongOrNull()
            ?: throw IOException("No word count for ${dictionary.path}: Content-Range $range")
    }

    override suspend fun fetchPage(dictionary: Dictionary, afterRowId: Long, limit: Int): List<DownloadedWord> {
        val url = table()
            .addQueryParameter("select", "id,external_id,stress,translation,redirect_to")
            .addQueryParameter("direction", "eq.${dictionary.path}")
            .addQueryParameter("id", "gt.$afterRowId")
            .addQueryParameter("order", "id.asc")
            .addQueryParameter("limit", limit.toString())
            .build()
        val rows = json.decodeFromString<List<DownloadRowDto>>(client.fetchString(request(url)))
        return rows.map { it.toWord() }
    }

    private fun table(): HttpUrl.Builder = projectUrl.newBuilder().addPathSegments("rest/v1/main_word")

    private fun request(url: HttpUrl): Request = Request.Builder()
        .url(url)
        .header("apikey", apiKey)
        .header("Authorization", "Bearer $apiKey")
        .build()
}
