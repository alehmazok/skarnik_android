package by.mazokaleh.skarnik.feature.offline.domain

/** What the Settings row of one dictionary shows. */
sealed interface OfflineDictionaryStatus {
    data object NotDownloaded : OfflineDictionaryStatus

    data class Downloading(val progress: DownloadProgress?) : OfflineDictionaryStatus

    data class Downloaded(val wordCount: Long) : OfflineDictionaryStatus

    /** The last download did not finish; starting again resumes it. */
    data object Failed : OfflineDictionaryStatus

    companion object {
        /**
         * A running job wins. Otherwise an unfinished download (failed job, or a cursor left with words already
         * stored, say after the job record was pruned) is a failure; a stored dictionary without a cursor is
         * complete. A cursor with nothing stored is stale: the download starts over, as in Flutter.
         */
        fun of(job: DownloadJob?, wordCount: Long, cursor: Long?): OfflineDictionaryStatus = when {
            job is DownloadJob.Active -> Downloading(job.progress)
            job is DownloadJob.Failed -> Failed
            wordCount > 0 && cursor != null -> Failed
            wordCount > 0 -> Downloaded(wordCount)
            else -> NotDownloaded
        }
    }
}
