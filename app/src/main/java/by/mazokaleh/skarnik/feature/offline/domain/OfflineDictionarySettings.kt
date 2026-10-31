package by.mazokaleh.skarnik.feature.offline.domain

import by.mazokaleh.skarnik.core.model.Dictionary
import kotlinx.coroutines.flow.Flow

/** The small values offline downloads keep between launches. */
interface OfflineDictionarySettings {
    /** The row id the last unfinished download of [dictionary] stopped after; null when none is unfinished. */
    fun cursor(dictionary: Dictionary): Flow<Long?>

    suspend fun setCursor(dictionary: Dictionary, rowId: Long)

    suspend fun clearCursor(dictionary: Dictionary)

    /** When downloads were started recently, in epoch milliseconds. */
    suspend fun recentDownloadAttempts(): List<Long>

    suspend fun setRecentDownloadAttempts(attempts: List<Long>)

    /** Whether Settings were opened since the offline dictionaries badge appeared. */
    val promoSeen: Flow<Boolean>

    suspend fun markPromoSeen()
}
