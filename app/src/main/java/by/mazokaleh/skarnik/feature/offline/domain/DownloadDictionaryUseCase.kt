package by.mazokaleh.skarnik.feature.offline.domain

import by.mazokaleh.skarnik.core.model.Dictionary
import java.io.IOException
import javax.inject.Inject
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow

/**
 * Downloads a whole dictionary page by page into the translation store, resuming after the last saved page
 * (Flutter's `DownloadDictionaryUseCase` and `SupabaseTranslationRepositoryImpl.downloadDictionary`).
 *
 * The cursor is saved after each page is written, so a download stopped at any point loses at most one page,
 * and rewriting a page is harmless (stored words are replaced). Each request is retried a few times on network
 * and server errors: one transient failure in a few hundred requests must not abort the whole download.
 */
class DownloadDictionaryUseCase @Inject constructor(
    private val source: OfflineDictionarySource,
    private val store: OfflineDictionaryStore,
    private val settings: OfflineDictionarySettings
) {
    operator fun invoke(dictionary: Dictionary): Flow<DownloadProgress> = flow {
        val total = withRetry { source.countWords(dictionary) }
        val stored = store.count(dictionary)
        var cursor = settings.cursor(dictionary).first() ?: 0
        // A cursor with nothing stored (storage wiped outside the app) would skip the gap forever.
        if (cursor > 0 && stored == 0L) {
            settings.clearCursor(dictionary)
            cursor = 0
        }
        // Starting from the first page rewrites everything, so words already stored are not counted as done.
        var done = if (cursor > 0) stored else 0
        emit(DownloadProgress(done, total))

        while (true) {
            val page = withRetry { source.fetchPage(dictionary, afterRowId = cursor, limit = PAGE_SIZE) }
            if (page.isEmpty()) break
            store.putAll(dictionary, page)
            done += page.size
            cursor = page.last().rowId
            settings.setCursor(dictionary, cursor)
            emit(DownloadProgress(done, total))
        }
        settings.clearCursor(dictionary)
    }

    private suspend fun <T> withRetry(block: suspend () -> T): T {
        var attempt = 1
        while (true) {
            try {
                return block()
            } catch (e: IOException) {
                if (attempt >= MAX_ATTEMPTS) throw e
                delay(RETRY_STEP * attempt)
                attempt++
            }
        }
    }

    companion object {
        /** Rows asked for per request, as in Flutter; Supabase may cap a response lower. */
        const val PAGE_SIZE = 5000
        const val MAX_ATTEMPTS = 5
        private val RETRY_STEP = 500.milliseconds
    }
}
