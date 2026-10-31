package by.mazokaleh.skarnik.core.network

import java.io.IOException
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.suspendCancellableCoroutine
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
 */
suspend fun <T> OkHttpClient.fetch(request: Request, read: (Response) -> T): T =
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
