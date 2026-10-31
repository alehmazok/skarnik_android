package by.mazokaleh.skarnik.core.network

import android.content.Context
import android.util.Log
import by.mazokaleh.skarnik.core.common.IoDispatcher
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext

/**
 * Deletes the Flutter app's HTTP cache, `databases/DioCache.db` (dio_http_cache's sqflite database). It is only
 * a cache and the native app caches in `cache/http`, so it is dropped, not migrated (parity 0.3a). Runs at every
 * start: once the file is gone this is a single failed lookup.
 */
class FlutterHttpCacheCleaner @Inject constructor(
    @ApplicationContext private val context: Context,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) {
    suspend operator fun invoke() {
        withContext(ioDispatcher) {
            // deleteDatabase also removes the -journal, -shm and -wal files next to it.
            if (context.getDatabasePath(DATABASE).exists() && context.deleteDatabase(DATABASE)) {
                Log.i(TAG, "Deleted Flutter's HTTP cache")
            }
        }
    }

    private companion object {
        const val TAG = "FlutterHttpCacheCleaner"
        const val DATABASE = "DioCache.db"
    }
}
