package by.mazokaleh.skarnik.core.network

import kotlin.time.Duration
import okhttp3.Interceptor
import okhttp3.Response

/**
 * Makes successful GET responses cacheable for [lifetime], whatever the server says, like Flutter's
 * `DioCacheManager` with `buildCacheOptions(maxAge)`. Neither the admin API nor Supabase sends cache headers
 * meant for this. [lifetime] is read for every response (Remote Config, parity 8.8), so a new value applies to
 * responses fetched after it, as in Flutter.
 */
class CacheLifetimeInterceptor(private val lifetime: () -> Duration) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val response = chain.proceed(chain.request())
        if (chain.request().method != "GET" || !response.isSuccessful) return response
        return response.newBuilder()
            .removeHeader("Pragma")
            .header("Cache-Control", "public, max-age=${lifetime().inWholeSeconds}")
            .build()
    }
}
