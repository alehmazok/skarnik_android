package by.mazokaleh.skarnik.feature.offline.domain

import by.mazokaleh.skarnik.core.model.Dictionary
import java.io.IOException
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.junit.Test

class DownloadDictionaryUseCaseTest {
    private val rows = (1L..5L).map(::downloadedWord)
    private val source = FakeOfflineDictionarySource(rows)
    private val store = FakeOfflineDictionaryStore()
    private val settings = FakeOfflineDictionarySettings()
    private val download = DownloadDictionaryUseCase(source, store, settings)

    @Test
    fun `downloads every page, reports progress and clears the cursor at the end`() = runTest {
        val progress = download(Dictionary.Tsbm).toList()

        assertEquals(listOf(0L, 2L, 4L, 5L), progress.map { it.done })
        assertEquals(5L, progress.last().total)
        assertEquals(5, store.count(Dictionary.Tsbm))
        assertEquals(listOf(0L, 2L, 4L, 5L), source.requestedCursors)
        assertEquals(listOf(2L, 4L, 5L), settings.savedCursors)
        assertNull(settings.cursor(Dictionary.Tsbm).first())
    }

    @Test
    fun `resumes after the saved cursor and counts what is already stored`() = runTest {
        store.putAll(Dictionary.Tsbm, rows.take(2))
        settings.setCursor(Dictionary.Tsbm, 2)

        val progress = download(Dictionary.Tsbm).toList()

        assertEquals(2L, source.requestedCursors.first())
        assertEquals(listOf(2L, 4L, 5L), progress.map { it.done })
        assertEquals(5, store.count(Dictionary.Tsbm))
    }

    @Test
    fun `a cursor with nothing stored is stale and the download starts over`() = runTest {
        settings.setCursor(Dictionary.Tsbm, 4)

        download(Dictionary.Tsbm).toList()

        assertEquals(0L, source.requestedCursors.first())
        assertEquals(5, store.count(Dictionary.Tsbm))
    }

    @Test
    fun `starting from the first page does not count words stored without a cursor`() = runTest {
        store.putAll(Dictionary.Tsbm, rows.take(3))

        val progress = download(Dictionary.Tsbm).toList()

        assertEquals(0L, progress.first().done)
        assertEquals(5L, progress.last().done)
    }

    @Test
    fun `transient network errors are retried`() = runTest {
        source.failures = DownloadDictionaryUseCase.MAX_ATTEMPTS - 1

        download(Dictionary.Tsbm).toList()

        assertEquals(5, store.count(Dictionary.Tsbm))
    }

    @Test
    fun `a page that keeps failing stops the download and keeps the cursor for a resume`() = runTest {
        val flow = download(Dictionary.Tsbm)
        assertFailsWith<IOException> {
            flow.collect { progress ->
                if (progress.done == 2L) source.failures = DownloadDictionaryUseCase.MAX_ATTEMPTS
            }
        }

        assertEquals(2L, settings.cursor(Dictionary.Tsbm).first())
        assertEquals(2, store.count(Dictionary.Tsbm))
    }

    @Test
    fun `only the requested dictionary is written`() = runTest {
        download(Dictionary.BelRus).toList()

        assertEquals(5, store.count(Dictionary.BelRus))
        assertEquals(0, store.count(Dictionary.Tsbm))
    }
}
