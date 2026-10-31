package by.mazokaleh.skarnik.feature.stress.data

import by.mazokaleh.skarnik.core.network.fetchString
import by.mazokaleh.skarnik.feature.stress.domain.StressBackend
import by.mazokaleh.skarnik.feature.stress.domain.StressRow
import by.mazokaleh.skarnik.feature.stress.domain.StressSource
import by.mazokaleh.skarnik.feature.stress.domain.StressWordEntry
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import org.jsoup.Jsoup

/**
 * starnik.by: `GET /api/wordList?lemma=<word>` (JSON) and `GET /pravapis/<id>` (an HTML page whose
 * `.wrapper table` is the stress table). Here `lemma` is the unstressed form and `word` the stressed one.
 */
class StarnikStressBackend(private val client: OkHttpClient, private val json: Json, private val baseUrl: HttpUrl) :
    StressBackend {
    override suspend fun resolveWordList(word: String): List<StressWordEntry> {
        val url = baseUrl.newBuilder()
            .addPathSegments("api/wordList")
            .addQueryParameter("lemma", word)
            .build()
        val body = client.fetchString(Request.Builder().url(url).build())
        return json.decodeFromString<StarnikWordListDto>(body).wordList.map {
            StressWordEntry(
                it.id,
                lemma = it.lemma,
                word = it.word,
                tableName = it.tableName,
                source = StressSource.Starnik
            )
        }
    }

    override suspend fun stressTable(id: Long): List<StressRow> {
        val url = baseUrl.newBuilder().addPathSegments("pravapis/$id").build()
        return parseStarnikTable(client.fetchString(Request.Builder().url(url).build()))
    }
}

/** Rows of the first table in `.wrapper`; a row counts only with exactly two cells (Flutter, iOS). */
internal fun parseStarnikTable(html: String): List<StressRow> {
    val document = Jsoup.parse(html)
    // Cells are kept as HTML; pretty printing would add line breaks and indents to it.
    document.outputSettings().prettyPrint(false)
    val table = document.selectFirst(".wrapper table") ?: return emptyList()
    return table.select("tr").mapNotNull { row ->
        val cells = row.select("td")
        if (cells.size == 2) StressRow(title = cells[0].html(), content = cells[1].html()) else null
    }
}

/**
 * skarnik_admin: `GET /api/stress_words/?word=<word>` and `GET /api/stress_words/<id>/`. Its field names are
 * the other way round from starnik.by (`word` is unstressed, `lemma` stressed), so they are swapped here.
 */
class AdminStressBackend(private val client: OkHttpClient, private val json: Json, private val baseUrl: HttpUrl) :
    StressBackend {
    override suspend fun resolveWordList(word: String): List<StressWordEntry> {
        val url = baseUrl.newBuilder()
            .addPathSegments("api/stress_words/")
            .addQueryParameter("word", word.lowercase())
            .build()
        val body = client.fetchString(Request.Builder().url(url).build())
        return json.decodeFromString<List<AdminStressWordDto>>(body).map {
            StressWordEntry(
                it.id,
                lemma = it.word,
                word = it.lemma,
                tableName = it.tableName,
                source = StressSource.Admin
            )
        }
    }

    override suspend fun stressTable(id: Long): List<StressRow> {
        val url = baseUrl.newBuilder().addPathSegments("api/stress_words/$id/").build()
        val body = client.fetchString(Request.Builder().url(url).build())
        return json.decodeFromString<AdminStressTableDto>(body).rows.map { StressRow(it.title, it.content) }
    }
}

@Serializable
internal data class StarnikWordListDto(@SerialName("word_list") val wordList: List<StarnikWordDto>)

@Serializable
internal data class StarnikWordDto(
    val id: Long,
    val lemma: String,
    val word: String,
    @SerialName("table_name") val tableName: String? = null
)

@Serializable
internal data class AdminStressWordDto(
    val id: Long,
    val word: String,
    val lemma: String,
    @SerialName("table_name") val tableName: String? = null
)

@Serializable
internal data class AdminStressTableDto(val rows: List<AdminStressRowDto>)

@Serializable
internal data class AdminStressRowDto(val title: String, val content: String)
