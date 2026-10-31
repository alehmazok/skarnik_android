package by.mazokaleh.skarnik.feature.offline.domain

import by.mazokaleh.skarnik.core.model.Dictionary
import kotlinx.coroutines.flow.Flow

data class DownloadProgress(val done: Long, val total: Long) {
    val fraction: Float get() = if (total <= 0) 0f else (done.toFloat() / total).coerceIn(0f, 1f)
}

/** The state of the background job that downloads one dictionary. */
sealed interface DownloadJob {
    /** Waiting to run or running. [progress] is null until the first count arrives. */
    data class Active(val progress: DownloadProgress?) : DownloadJob

    data object Failed : DownloadJob

    /** Succeeded or was stopped for good; the store and the cursor tell what is left. */
    data object Finished : DownloadJob
}

/** Runs downloads in the background, so they outlive the screen and the process. */
interface OfflineDownloads {
    /** Starts downloading [dictionary], or resumes it from its cursor. Does nothing if one is already running. */
    fun start(dictionary: Dictionary)

    /** The latest download job of [dictionary]; null if none ran recently. */
    fun observe(dictionary: Dictionary): Flow<DownloadJob?>
}
