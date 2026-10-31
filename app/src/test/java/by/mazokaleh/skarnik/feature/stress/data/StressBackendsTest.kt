package by.mazokaleh.skarnik.feature.stress.data

import by.mazokaleh.skarnik.core.network.HttpException
import by.mazokaleh.skarnik.feature.stress.domain.StressRow
import by.mazokaleh.skarnik.feature.stress.domain.StressSource
import by.mazokaleh.skarnik.feature.stress.domain.StressWordEntry
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import org.junit.After
import org.junit.Before
import org.junit.Test

class StressBackendsTest {
    private val server = MockWebServer()
    private val client = OkHttpClient()
    private val json = Json { ignoreUnknownKeys = true }
    private lateinit var baseUrl: HttpUrl

    @Before
    fun setUp() {
        server.start()
        baseUrl = server.url("/")
    }

    @After
    fun tearDown() {
        server.close()
    }

    private fun respond(body: String, code: Int = 200) {
        server.enqueue(MockResponse.Builder().code(code).body(body).build())
    }

    @Test
    fun `starnik word list keeps its field meaning`() = runTest {
        respond(
            """{"word_list":[{"id":123,"lemma":"пара","word":"па́ра","table_name":"Nouns","extra":1},""" +
                """{"id":124,"lemma":"пара","word":"пара́","table_name":null}]}"""
        )

        val words = StarnikStressBackend(client, json, baseUrl).resolveWordList("пара")

        assertEquals(
            listOf(
                StressWordEntry(123, "пара", "па́ра", "Nouns", StressSource.Starnik),
                StressWordEntry(124, "пара", "пара́", null, StressSource.Starnik)
            ),
            words
        )
        val request = server.takeRequest()
        assertEquals("/api/wordList", request.url.encodedPath)
        assertEquals("пара", request.url.queryParameter("lemma"))
    }

    @Test
    fun `starnik table is parsed from the page`() = runTest {
        respond(
            """<html><body><table><tr><td>не тая</td><td>табліца</td></tr></table>""" +
                """<div class="wrapper"><h1>пара</h1><table>""" +
                """<tr><th>Склон</th><th>Формы</th></tr>""" +
                """<tr><td>Назоўны<br><span class="pytanne">хто? што?</span></td><td><b> па́ра </b></td></tr>""" +
                """<tr><td>лішні</td><td>a</td><td>b</td></tr>""" +
                """<tr><td>Родны</td><td><b> па́ры </b></td></tr>""" +
                """</table></div></body></html>"""
        )

        val rows = StarnikStressBackend(client, json, baseUrl).stressTable(42)

        assertEquals(
            listOf(
                StressRow("Назоўны<br><span class=\"pytanne\">хто? што?</span>", "<b> па́ра </b>"),
                StressRow("Родны", "<b> па́ры </b>")
            ),
            rows
        )
        assertEquals("/pravapis/42", server.takeRequest().url.encodedPath)
    }

    @Test
    fun `starnik page without the table has no rows`() {
        assertEquals(emptyList(), parseStarnikTable("<html><body><p>Not found</p></body></html>"))
    }

    @Test
    fun `admin word list swaps word and lemma and asks in lower case`() = runTest {
        respond("""[{"id":7,"word":"пара","lemma":"пара́","table_name":"Nouns"}]""")

        val words = AdminStressBackend(client, json, baseUrl).resolveWordList("Пара")

        assertEquals(listOf(StressWordEntry(7, "пара", "пара́", "Nouns", StressSource.Admin)), words)
        val request = server.takeRequest()
        assertEquals("/api/stress_words/", request.url.encodedPath)
        assertEquals("пара", request.url.queryParameter("word"))
    }

    @Test
    fun `admin table reads the rows`() = runTest {
        respond("""{"id":7,"rows":[{"title":"Форма","content":"<b> пара́ </b>"}]}""")

        val rows = AdminStressBackend(client, json, baseUrl).stressTable(7)

        assertEquals(listOf(StressRow("Форма", "<b> пара́ </b>")), rows)
        assertEquals("/api/stress_words/7/", server.takeRequest().url.encodedPath)
    }

    @Test
    fun `admin errors are thrown`() = runTest {
        respond("""{"detail":"Not found."}""", code = 404)

        val error = assertFailsWith<HttpException> { AdminStressBackend(client, json, baseUrl).stressTable(1) }
        assertEquals(404, error.code)
    }
}
