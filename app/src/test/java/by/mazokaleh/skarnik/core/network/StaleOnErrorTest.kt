package by.mazokaleh.skarnik.core.network

import java.io.IOException
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.time.Duration
import kotlinx.coroutines.test.runTest
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import okhttp3.Cache
import okhttp3.OkHttpClient
import okhttp3.Request
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class StaleOnErrorTest {
    @get:Rule
    val tmp = TemporaryFolder()

    private val server = MockWebServer()
    private lateinit var client: OkHttpClient

    /** A zero lifetime stores every response already stale, as a cached word is after 24 hours. */
    @Before
    fun setUp() {
        server.start()
        client = OkHttpClient.Builder()
            .cache(Cache(tmp.newFolder(), 1024L * 1024))
            .addNetworkInterceptor(CacheLifetimeInterceptor(Duration.ZERO))
            .build()
    }

    @After
    fun tearDown() {
        server.close()
        client.cache?.close()
    }

    @Test
    fun `offline, a stale cached translation is served`() = runTest {
        server.enqueue(MockResponse.Builder().body("cached").build())
        val request = Request.Builder().url(server.url("/api/words/belrus/1/")).build()
        assertEquals("cached", client.fetchString(request))

        server.close()

        assertEquals("cached", client.fetchString(request))
    }

    @Test
    fun `online, a stale entry is fetched again`() = runTest {
        server.enqueue(MockResponse.Builder().body("first").build())
        server.enqueue(MockResponse.Builder().body("second").build())
        val request = Request.Builder().url(server.url("/api/words/belrus/1/")).build()

        assertEquals("first", client.fetchString(request))
        assertEquals("second", client.fetchString(request))
    }

    @Test
    fun `offline without a cached copy, the network error is thrown`() = runTest {
        val request = Request.Builder().url(server.url("/api/words/belrus/2/")).build()
        server.close()

        val error = assertFailsWith<IOException> { client.fetchString(request) }
        assertFalse(error is HttpException)
    }

    @Test
    fun `an HTTP error is not masked by a stale copy`() = runTest {
        server.enqueue(MockResponse.Builder().body("cached").build())
        server.enqueue(MockResponse.Builder().code(500).build())
        val request = Request.Builder().url(server.url("/api/words/belrus/1/")).build()
        assertEquals("cached", client.fetchString(request))

        val error = assertFailsWith<HttpException> { client.fetchString(request) }
        assertEquals(500, error.code)
    }
}
