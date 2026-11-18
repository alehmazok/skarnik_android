package by.mazokaleh.skarnik.core.network

import java.io.IOException
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.suspendCancellableCoroutine
import okhttp3.CacheControl
import okhttp3.Call
import okhttp3.Callback
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response

/**
 * Runs [request] on OkHttp's own threads and returns the body; cancelling the coroutine cancels the call.
 * Throws [HttpException] for a non-2xx response.
 */
suspend fun OkHttpClient.fetchString(request: Request): String = fetch(request) { it.body.string() }

/**
 * Like [fetchString], but [read] gets the whole successful response (headers and body), on OkHttp's thread.
 *
 * Stale on error: when a GET fails without an HTTP answer (offline, timeout, DNS), it is retried once from the
 * HTTP cache only, so a word opened before shows even after its 24 h lifetime. Without a cached copy, or for an
 * HTTP error status, the original exception is thrown.
 */
suspend fun <T> OkHttpClient.fetch(request: Request, read: (Response) -> T): T = try {
    call(request, read)
} catch (e: IOException) {
    if (e is HttpException || cache == null || request.method != "GET") throw e
    try {
        call(request.newBuilder().cacheControl(CacheControl.FORCE_CACHE).build(), read)
    } catch (ignored: IOException) {
        throw e
    }
}

private suspend fun <T> OkHttpClient.call(request: Request, read: (Response) -> T): T =
    suspendCancellableCoroutine { continuation ->
        val call = newCall(request)
        continuation.invokeOnCancellation { call.cancel() }
        call.enqueue(
            object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    continuation.resumeWithException(e)
                }

                override fun onResponse(call: Call, response: Response) {
                    val result = response.use {
                        if (it.isSuccessful) {
                            runCatching { read(it) }
                        } else {
                            Result.failure(HttpException(it.code, request.url.toString()))
                        }
                    }
                    continuation.resumeWith(result)
                }
            }
        )
    }

class HttpException(val code: Int, url: String) : IOException("HTTP $code for $url")
