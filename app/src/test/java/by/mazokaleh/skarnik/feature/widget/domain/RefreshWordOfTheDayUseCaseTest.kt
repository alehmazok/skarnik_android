package by.mazokaleh.skarnik.feature.widget.domain

import by.mazokaleh.skarnik.core.common.WallClock
import by.mazokaleh.skarnik.core.model.Dictionary
import by.mazokaleh.skarnik.core.model.Word
import by.mazokaleh.skarnik.feature.translation.domain.GetTranslationUseCase
import by.mazokaleh.skarnik.feature.translation.domain.LocalTranslationSource
import by.mazokaleh.skarnik.feature.translation.domain.RemoteTranslationSource
import by.mazokaleh.skarnik.feature.translation.domain.TranslationEntry
import java.io.IOException
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlinx.coroutines.test.runTest
import org.junit.Test

class RefreshWordOfTheDayUseCaseTest {
    private val now = 1_800_000_000_000L
    private val day = 24L * 60 * 60 * 1000

    private fun word(id: Long, text: String) = Word(Dictionary.BelRus, id, text, text.take(1), text, null)

    private val bursztyn = word(1, "бурштын")
    private val vada = word(2, "вада")
    private val kot = word(3, "кот")

    private val translations = mapOf(1L to "<b>янтарь</b>", 2L to "<p>вода</p>", 3L to "<p>кошка</p>")

    private class FakeHistory : WidgetHistoryRepository {
        val entries = mutableMapOf<Long, WidgetHistoryEntry>()

        override suspend fun entry(wordId: Long) = entries[wordId]

        override suspend fun save(wordId: Long, entry: WidgetHistoryEntry) {
            entries[wordId] = entry
        }
    }

    private val history = FakeHistory()

    private fun useCase(
        picks: List<Word?>,
        api: RemoteTranslationSource = RemoteTranslationSource { _, id ->
            TranslationEntry(null, translations.getValue(id), null)
        }
    ): Pair<RefreshWordOfTheDayUseCase, () -> Int> {
        var picked = 0
        val source = RandomWordSource { picks[(picked++).coerceAtMost(picks.lastIndex)] }
        val failing = RemoteTranslationSource { _, _ -> throw IOException("offline") }
        val getTranslation = GetTranslationUseCase(LocalTranslationSource { _, _ -> null }, api, failing)
        return RefreshWordOfTheDayUseCase(source, history, getTranslation, WallClock { now }) to { picked }
    }

    @Test
    fun `a new word is shown as plain text and recorded`() = runTest {
        val (refresh, _) = useCase(listOf(bursztyn))

        assertEquals(WordOfTheDay(1, "бурштын", "янтарь"), refresh())
        assertEquals(WidgetHistoryEntry(now, similar = false), history.entries[1])
    }

    @Test
    fun `a word too close to its translation is recorded as similar and skipped`() = runTest {
        val (refresh, _) = useCase(listOf(vada, bursztyn))

        assertEquals(1L, refresh()?.wordId)
        assertEquals(WidgetHistoryEntry(now, similar = true), history.entries[2])
    }

    @Test
    fun `a word shown in the last 30 days is skipped without fetching it`() = runTest {
        history.entries[1] = WidgetHistoryEntry(now - 29 * day, similar = false)
        val (refresh, _) = useCase(
            listOf(bursztyn, kot),
            api = { _, id -> if (id == 1L) error("not fetched") else TranslationEntry(null, "кошка", null) }
        )

        assertEquals(3L, refresh()?.wordId)
    }

    @Test
    fun `an older word can be shown again, unless it was similar`() = runTest {
        history.entries[1] = WidgetHistoryEntry(now - 31 * day, similar = false)
        history.entries[2] = WidgetHistoryEntry(now - 400 * day, similar = true)
        val (refresh, _) = useCase(listOf(vada, bursztyn))

        assertEquals(1L, refresh()?.wordId)
        assertEquals(WidgetHistoryEntry(now - 400 * day, similar = true), history.entries[2])
    }

    @Test
    fun `a failed translation tries another word and records nothing`() = runTest {
        val (refresh, _) = useCase(
            listOf(kot, bursztyn),
            api = { _, id -> if (id == 3L) throw IOException("offline") else TranslationEntry(null, "янтарь", null) }
        )

        assertEquals(1L, refresh()?.wordId)
        assertNull(history.entries[3])
    }

    @Test
    fun `a redirect is skipped`() = runTest {
        val (refresh, _) = useCase(
            listOf(kot, bursztyn),
            api = { _, id -> TranslationEntry(null, "янтарь", if (id == 3L) "/belrus/1" else null) }
        )

        assertEquals(1L, refresh()?.wordId)
    }

    @Test
    fun `gives up after 30 attempts`() = runTest {
        val (refresh, picked) = useCase(listOf(vada))

        assertNull(refresh())
        assertEquals(RefreshWordOfTheDayUseCase.MAX_ATTEMPTS, picked())
    }

    @Test
    fun `an empty index gives no word`() = runTest {
        val (refresh, picked) = useCase(listOf(null))

        assertNull(refresh())
        assertEquals(1, picked())
    }

    @Test
    fun `the fallback word has no link, others link to skarnik_app`() {
        assertNull(WordOfTheDay.Fallback.link)
        assertEquals("https://skarnik.app/r/belrus/27890", WordOfTheDay(27890, "бурштын", "янтарь").link)
    }
}
