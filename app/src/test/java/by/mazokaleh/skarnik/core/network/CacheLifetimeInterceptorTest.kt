package by.mazokaleh.skarnik.core.network

import kotlin.test.assertEquals
import kotlin.time.Duration.Companion.hours
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

class CacheLifetimeInterceptorTest {
    @get:Rule
    val tmp = TemporaryFolder()

    private val server = MockWebServer()
    private lateinit var client: OkHttpClient

    @Before
    fun setUp() {
        server.start()
        client = OkHttpClient.Builder()
            .cache(Cache(tmp.newFolder("http"), 1024L * 1024))
            .addNetworkInterceptor(CacheLifetimeInterceptor(24.hours))
            .build()
    }

    @After
    fun tearDown() {
        server.close()
        client.cache?.close()
    }

    @Test
    fun `a translation is served from the cache even when the server forbids caching`() = runTest {
        server.enqueue(MockResponse.Builder().addHeader("Cache-Control", "no-cache").body("first").build())
        server.enqueue(MockResponse.Builder().body("second").build())
        val request = Request.Builder().url(server.url("/api/words/belrus/1/")).build()

        assertEquals("first", client.fetchString(request))
        assertEquals("first", client.fetchString(request))
        assertEquals(1, server.requestCount)
    }

    @Test
    fun `errors are not cached`() = runTest {
        server.enqueue(MockResponse.Builder().code(500).body("oops").build())
        server.enqueue(MockResponse.Builder().body("ok").build())
        val request = Request.Builder().url(server.url("/api/words/belrus/1/")).build()

        runCatching { client.fetchString(request) }
        assertEquals("ok", client.fetchString(request))
    }
}
