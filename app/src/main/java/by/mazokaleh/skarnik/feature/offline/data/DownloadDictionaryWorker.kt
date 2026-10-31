package by.mazokaleh.skarnik.feature.offline.data

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import by.mazokaleh.skarnik.core.common.suspendRunCatching
import by.mazokaleh.skarnik.core.model.Dictionary
import by.mazokaleh.skarnik.feature.offline.domain.DownloadDictionaryUseCase
import by.mazokaleh.skarnik.feature.offline.domain.DownloadProgress
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

/**
 * Downloads one dictionary and reports progress. If the system stops it, WorkManager runs it again later and
 * the download resumes from its cursor.
 */
@HiltWorker
class DownloadDictionaryWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val download: DownloadDictionaryUseCase
) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val dictionary = inputData.getString(KEY_DICTIONARY)?.let(Dictionary::byPath) ?: return Result.failure()
        return suspendRunCatching {
            download(dictionary).collect { setProgress(it.toData()) }
        }.fold(
            onSuccess = { Result.success() },
            onFailure = {
                Log.e(TAG, "Cannot download ${dictionary.path}", it)
                Result.failure()
            }
        )
    }

    companion object {
        const val KEY_DICTIONARY = "dictionary"
        private const val KEY_DONE = "done"
        private const val KEY_TOTAL = "total"
        private const val TAG = "DownloadDictionary"

        fun input(dictionary: Dictionary): Data = workDataOf(KEY_DICTIONARY to dictionary.path)

        private fun DownloadProgress.toData(): Data = workDataOf(KEY_DONE to done, KEY_TOTAL to total)

        /** The progress a running job reported last, or null before its first report. */
        fun progressOf(data: Data): DownloadProgress? {
            if (!data.hasKeyWithValueOfType(KEY_TOTAL, Long::class.javaObjectType)) return null
            return DownloadProgress(done = data.getLong(KEY_DONE, 0), total = data.getLong(KEY_TOTAL, 0))
        }
    }
}
