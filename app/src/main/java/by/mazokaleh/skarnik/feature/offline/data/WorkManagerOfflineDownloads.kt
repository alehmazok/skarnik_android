package by.mazokaleh.skarnik.feature.offline.data

import android.content.Context
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import by.mazokaleh.skarnik.core.model.Dictionary
import by.mazokaleh.skarnik.feature.offline.domain.DownloadJob
import by.mazokaleh.skarnik.feature.offline.domain.OfflineDownloads
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

/** One unique job per dictionary, so a second tap or a second screen never starts a parallel download. */
class WorkManagerOfflineDownloads @Inject constructor(@ApplicationContext private val context: Context) :
    OfflineDownloads {
    private val workManager: WorkManager get() = WorkManager.getInstance(context)

    override fun start(dictionary: Dictionary) {
        val request = OneTimeWorkRequestBuilder<DownloadDictionaryWorker>()
            .setInputData(DownloadDictionaryWorker.input(dictionary))
            .build()
        workManager.enqueueUniqueWork(workName(dictionary), ExistingWorkPolicy.KEEP, request)
    }

    override fun observe(dictionary: Dictionary): Flow<DownloadJob?> = workManager
        .getWorkInfosForUniqueWorkFlow(workName(dictionary))
        .map { infos -> infos.lastOrNull()?.toJob() }
        .distinctUntilChanged()

    private fun WorkInfo.toJob(): DownloadJob = when (state) {
        WorkInfo.State.ENQUEUED, WorkInfo.State.RUNNING, WorkInfo.State.BLOCKED ->
            DownloadJob.Active(DownloadDictionaryWorker.progressOf(progress))
        WorkInfo.State.FAILED -> DownloadJob.Failed
        WorkInfo.State.SUCCEEDED, WorkInfo.State.CANCELLED -> DownloadJob.Finished
    }

    private fun workName(dictionary: Dictionary) = "offline_dictionary_${dictionary.path}"
}
