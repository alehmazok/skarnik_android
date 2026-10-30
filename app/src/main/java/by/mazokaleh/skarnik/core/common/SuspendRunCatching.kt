package by.mazokaleh.skarnik.core.common

import kotlin.coroutines.cancellation.CancellationException

/** Like [runCatching], but rethrows [CancellationException] so coroutine cancellation keeps working. */
@Suppress("TooGenericExceptionCaught")
inline fun <T> suspendRunCatching(block: () -> T): Result<T> = try {
    Result.success(block())
} catch (e: CancellationException) {
    throw e
} catch (e: Throwable) {
    Result.failure(e)
}
