package by.mazokaleh.skarnik.feature.offline.domain

import by.mazokaleh.skarnik.core.model.Dictionary
import java.io.IOException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

class FakeOfflineDictionarySettings : OfflineDictionarySettings {
    val cursors = MutableStateFlow<Map<Dictionary, Long>>(emptyMap())
    val savedCursors = mutableListOf<Long>()
    var attempts: List<Long> = emptyList()
    val seen = MutableStateFlow(false)

    override fun cursor(dictionary: Dictionary): Flow<Long?> = cursors.map { it[dictionary] }

    override suspend fun setCursor(dictionary: Dictionary, rowId: Long) {
        savedCursors += rowId
        cursors.value += dictionary to rowId
    }

    override suspend fun clearCursor(dictionary: Dictionary) {
        cursors.value -= dictionary
    }

    override suspend fun recentDownloadAttempts(): List<Long> = attempts

    override suspend fun setRecentDownloadAttempts(attempts: List<Long>) {
        this.attempts = attempts
    }

    override val promoSeen: Flow<Boolean> = seen

    override suspend fun markPromoSeen() {
        seen.value = true
    }
}

class FakeOfflineDictionaryStore : OfflineDictionaryStore {
    /** Stored words by dictionary and word id. */
    val words = MutableStateFlow<Map<Dictionary, Map<Long, DownloadedWord>>>(emptyMap())

    override suspend fun count(dictionary: Dictionary): Long = words.value[dictionary].orEmpty().size.toLong()

    override fun observeCount(dictionary: Dictionary): Flow<Long> = words.map { it[dictionary].orEmpty().size.toLong() }

    override suspend fun putAll(dictionary: Dictionary, words: List<DownloadedWord>) {
        val current = this.words.value[dictionary].orEmpty()
        this.words.value += dictionary to current + words.associateBy { it.wordId }
    }

    override suspend fun clear(dictionary: Dictionary) {
        words.value -= dictionary
    }
}

/** Serves [rows] in pages of [serverPageSize]; [failures] makes the next requests throw. */
class FakeOfflineDictionarySource(private val rows: List<DownloadedWord>, private val serverPageSize: Int = 2) :
    OfflineDictionarySource {
    var failures = 0
    val requestedCursors = mutableListOf<Long>()

    override suspend fun countWords(dictionary: Dictionary): Long {
        failIfAsked()
        return rows.size.toLong()
    }

    override suspend fun fetchPage(dictionary: Dictionary, afterRowId: Long, limit: Int): List<DownloadedWord> {
        failIfAsked()
        requestedCursors += afterRowId
        return rows.filter { it.rowId > afterRowId }.take(minOf(limit, serverPageSize))
    }

    private fun failIfAsked() {
        if (failures > 0) {
            failures--
            throw IOException("offline")
        }
    }
}

fun downloadedWord(rowId: Long) = DownloadedWord(
    rowId = rowId,
    wordId = rowId * 10,
    stress = null,
    translation = "<b>$rowId</b>",
    redirectTo = null
)
