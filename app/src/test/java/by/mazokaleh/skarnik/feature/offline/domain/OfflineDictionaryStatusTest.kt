package by.mazokaleh.skarnik.feature.offline.domain

import kotlin.test.assertEquals
import org.junit.Test

class OfflineDictionaryStatusTest {
    private val progress = DownloadProgress(done = 10, total = 100)

    @Test
    fun `a running job shows its progress whatever is stored`() {
        assertEquals(
            OfflineDictionaryStatus.Downloading(progress),
            OfflineDictionaryStatus.of(DownloadJob.Active(progress), wordCount = 10, cursor = 5)
        )
        assertEquals(
            OfflineDictionaryStatus.Downloading(null),
            OfflineDictionaryStatus.of(DownloadJob.Active(null), wordCount = 0, cursor = null)
        )
    }

    @Test
    fun `a failed job is a failure, even with nothing stored`() {
        assertEquals(OfflineDictionaryStatus.Failed, OfflineDictionaryStatus.of(DownloadJob.Failed, 0, null))
    }

    @Test
    fun `stored words with a cursor left are an unfinished download`() {
        assertEquals(OfflineDictionaryStatus.Failed, OfflineDictionaryStatus.of(null, wordCount = 10, cursor = 5))
        assertEquals(
            OfflineDictionaryStatus.Failed,
            OfflineDictionaryStatus.of(DownloadJob.Finished, wordCount = 10, cursor = 5)
        )
    }

    @Test
    fun `stored words without a cursor are a complete download`() {
        assertEquals(
            OfflineDictionaryStatus.Downloaded(10),
            OfflineDictionaryStatus.of(DownloadJob.Finished, wordCount = 10, cursor = null)
        )
    }

    @Test
    fun `nothing stored, or only a stale cursor, is not downloaded`() {
        assertEquals(OfflineDictionaryStatus.NotDownloaded, OfflineDictionaryStatus.of(null, 0, null))
        assertEquals(OfflineDictionaryStatus.NotDownloaded, OfflineDictionaryStatus.of(DownloadJob.Finished, 0, 7))
    }

    @Test
    fun `progress fraction stays within bounds`() {
        assertEquals(0f, DownloadProgress(5, 0).fraction)
        assertEquals(1f, DownloadProgress(120, 100).fraction)
        assertEquals(0.1f, progress.fraction)
    }
}
