package by.mazokaleh.skarnik.core.network

import kotlin.test.assertEquals
import kotlin.time.Duration
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
    private var lifetime = 24.hours
    private lateinit var client: OkHttpClient

    @Before
    fun setUp() {
        server.start()
        client = OkHttpClient.Builder()
            .cache(Cache(tmp.newFolder("http"), 1024L * 1024))
            .addNetworkInterceptor(CacheLifetimeInterceptor { lifetime })
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

    @Test
    fun `a lifetime of zero turns caching off`() = runTest {
        lifetime = Duration.ZERO
        server.enqueue(MockResponse.Builder().body("first").build())
        server.enqueue(MockResponse.Builder().body("second").build())
        val request = Request.Builder().url(server.url("/api/words/belrus/1/")).build()

        assertEquals("first", client.fetchString(request))
        assertEquals("second", client.fetchString(request))
    }

    @Test
    fun `a new lifetime applies to the next response`() = runTest {
        lifetime = Duration.ZERO
        server.enqueue(MockResponse.Builder().body("first").build())
        server.enqueue(MockResponse.Builder().body("second").build())
        server.enqueue(MockResponse.Builder().body("third").build())
        val request = Request.Builder().url(server.url("/api/words/belrus/1/")).build()

        assertEquals("first", client.fetchString(request))
        lifetime = 1.hours
        assertEquals("second", client.fetchString(request))
        assertEquals("second", client.fetchString(request))
        assertEquals(2, server.requestCount)
    }
}
