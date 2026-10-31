package by.mazokaleh.skarnik.feature.offline.data

import by.mazokaleh.skarnik.core.model.Dictionary
import by.mazokaleh.skarnik.core.network.HttpException
import by.mazokaleh.skarnik.feature.offline.domain.DownloadedWord
import by.mazokaleh.skarnik.feature.translation.data.toBaseUrl
import java.io.IOException
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import okhttp3.OkHttpClient
import org.junit.After
import org.junit.Before
import org.junit.Test

class SupabaseOfflineDictionarySourceTest {
    private val server = MockWebServer()
    private lateinit var source: SupabaseOfflineDictionarySource

    @Before
    fun setUp() {
        server.start()
        source = SupabaseOfflineDictionarySource(
            OkHttpClient(),
            Json { ignoreUnknownKeys = true },
            server.url("/").toString().toBaseUrl(),
            "anon-key"
        )
    }

    @After
    fun tearDown() {
        server.close()
    }

    @Test
    fun `counts a dictionary's words from Content-Range`() = runTest {
        server.enqueue(MockResponse.Builder().header("Content-Range", "0-0/103412").body("""[{"id":1}]""").build())

        assertEquals(103_412, source.countWords(Dictionary.BelRus))

        val request = server.takeRequest()
        assertEquals("/rest/v1/main_word", request.url.encodedPath)
        assertEquals("eq.belrus", request.url.queryParameter("direction"))
        assertEquals("count=exact", request.headers["Prefer"])
        assertEquals("anon-key", request.headers["apikey"])
        assertEquals("Bearer anon-key", request.headers["Authorization"])
    }

    @Test
    fun `an empty dictionary counts zero`() = runTest {
        server.enqueue(MockResponse.Builder().header("Content-Range", "*/0").body("[]").build())

        assertEquals(0, source.countWords(Dictionary.Tsbm))
    }

    @Test
    fun `a response without a count is an error`() = runTest {
        server.enqueue(MockResponse.Builder().body("[]").build())

        assertFailsWith<IOException> { source.countWords(Dictionary.Tsbm) }
    }

    @Test
    fun `fetches a page after the cursor in row order`() = runTest {
        server.enqueue(
            MockResponse.Builder().body(
                """[{"id":101,"external_id":27890,"stress":"буршты́н","translation":"<b>янтарь</b>",""" +
                    """"redirect_to":null},{"id":102,"external_id":5,"stress":null,"translation":null,""" +
                    """"redirect_to":"/belrus/7"}]"""
            ).build()
        )

        val page = source.fetchPage(Dictionary.BelRus, afterRowId = 100, limit = 5000)

        assertEquals(
            listOf(
                DownloadedWord(101, 27890, "буршты́н", "<b>янтарь</b>", null),
                DownloadedWord(102, 5, null, "", "/belrus/7")
            ),
            page
        )
        val url = server.takeRequest().url
        assertEquals("id,external_id,stress,translation,redirect_to", url.queryParameter("select"))
        assertEquals("eq.belrus", url.queryParameter("direction"))
        assertEquals("gt.100", url.queryParameter("id"))
        assertEquals("id.asc", url.queryParameter("order"))
        assertEquals("5000", url.queryParameter("limit"))
    }

    @Test
    fun `server errors are thrown as network errors, so they are retried`() = runTest {
        server.enqueue(MockResponse.Builder().code(500).body("""{"code":"57014"}""").build())

        val error = assertFailsWith<HttpException> { source.fetchPage(Dictionary.Tsbm, 0, 5000) }
        assertEquals(500, error.code)
    }
}
