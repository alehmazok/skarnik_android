package by.mazokaleh.skarnik.core.network

import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours
import okhttp3.Interceptor
import okhttp3.Response

/** How long a fetched translation is served from the HTTP cache. Flutter's Remote Config default. */
object HttpCacheLifetime {
    // TODO(parity 8.8): read `http_cache_duration_in_hours` from Remote Config.
    val DEFAULT: Duration = 24.hours
}

/**
 * Makes successful GET responses cacheable for [lifetime], whatever the server says, like Flutter's
 * `DioCacheManager` with `buildCacheOptions(maxAge)`. Neither the admin API nor Supabase sends cache headers
 * meant for this.
 */
class CacheLifetimeInterceptor(private val lifetime: Duration) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val response = chain.proceed(chain.request())
        if (chain.request().method != "GET" || !response.isSuccessful) return response
        return response.newBuilder()
            .removeHeader("Pragma")
            .header("Cache-Control", "public, max-age=${lifetime.inWholeSeconds}")
            .build()
    }
}
