package by.mazokaleh.skarnik.feature.translation.data

import by.mazokaleh.skarnik.core.model.Dictionary
import by.mazokaleh.skarnik.core.network.HttpException
import by.mazokaleh.skarnik.feature.translation.domain.TranslationEntry
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

class RemoteTranslationSourcesTest {
    private val server = MockWebServer()
    private val client = OkHttpClient()
    private val json = Json { ignoreUnknownKeys = true }

    @Before
    fun setUp() {
        server.start()
    }

    @After
    fun tearDown() {
        server.close()
    }

    private fun respond(body: String, code: Int = 200) {
        server.enqueue(MockResponse.Builder().code(code).body(body).build())
    }

    @Test
    fun `admin API reads a word`() = runTest {
        respond(
            """{"id":5,"external_id":27890,"direction":"be-ru","text":"бурштын","stress":"буршты́н",""" +
                """"translation":"<b>янтарь</b>","redirect_to":null}"""
        )
        val source = AdminApiTranslationSource(client, json, server.url("/").toString().toBaseUrl())

        val entry = source.fetch(Dictionary.BelRus, 27890)

        assertEquals(TranslationEntry("буршты́н", "<b>янтарь</b>", null), entry)
        assertEquals("/api/words/belrus/27890/", server.takeRequest().url.encodedPath)
    }

    @Test
    fun `admin API errors are thrown`() = runTest {
        respond("""{"detail":"Not found."}""", code = 404)
        val source = AdminApiTranslationSource(client, json, server.url("/").toString().toBaseUrl())

        val error = assertFailsWith<HttpException> { source.fetch(Dictionary.RusBel, 1) }
        assertEquals(404, error.code)
    }

    @Test
    fun `Supabase filters by id and direction and sends the key`() = runTest {
        respond("""[{"stress":null,"translation":null,"redirect_to":"/tsbm/3528"}]""")
        val source = SupabaseTranslationSource(client, json, server.url("/").toString().toBaseUrl(), "anon-key")

        val entry = source.fetch(Dictionary.Tsbm, 77)

        assertEquals(TranslationEntry(null, null, "/tsbm/3528"), entry)
        val request = server.takeRequest()
        assertEquals("/rest/v1/main_word", request.url.encodedPath)
        assertEquals("eq.77", request.url.queryParameter("external_id"))
        assertEquals("eq.tsbm", request.url.queryParameter("direction"))
        assertEquals("anon-key", request.headers["apikey"])
        assertEquals("Bearer anon-key", request.headers["Authorization"])
    }

    @Test
    fun `Supabase without a row is an error`() = runTest {
        respond("[]")
        val source = SupabaseTranslationSource(client, json, server.url("/").toString().toBaseUrl(), "anon-key")

        assertFailsWith<NoSuchElementException> { source.fetch(Dictionary.Tsbm, 77) }
    }
}
