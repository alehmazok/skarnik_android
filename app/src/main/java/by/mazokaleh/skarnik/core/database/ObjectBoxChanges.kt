package by.mazokaleh.skarnik.core.database

import io.objectbox.BoxStore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.conflate

/**
 * Emits once on collection and again after every committed transaction that changes entity [T] in this store,
 * wherever the change came from. Bursts are conflated: a slow collector re-reads once, not once per change.
 */
inline fun <reified T> BoxStore.changes(): Flow<Unit> = callbackFlow {
    val subscription = subscribe(T::class.java).onlyChanges().observer { trySend(Unit) }
    send(Unit)
    awaitClose { subscription.cancel() }
}.conflate()
