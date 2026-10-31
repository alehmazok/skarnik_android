package by.mazokaleh.skarnik.core.common

import com.google.android.gms.tasks.Task
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.suspendCancellableCoroutine

/**
 * Suspends until a Play services / Firebase [Task] completes. A failed or cancelled task throws. A small helper
 * instead of kotlinx-coroutines-play-services: one function does not need another dependency.
 */
suspend fun <T> Task<T>.await(): T = suspendCancellableCoroutine { continuation ->
    addOnCompleteListener { task ->
        if (task.isSuccessful) {
            continuation.resume(task.result)
        } else {
            continuation.resumeWithException(task.exception ?: IllegalStateException("Task was cancelled"))
        }
    }
}
