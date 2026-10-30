package by.mazokaleh.skarnik.feature.translation.domain

import by.mazokaleh.skarnik.core.model.Dictionary
import by.mazokaleh.skarnik.core.model.Word
import by.mazokaleh.skarnik.core.model.WordLink
import java.io.IOException
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlinx.coroutines.test.runTest
import org.junit.Test

class GetTranslationUseCaseTest {
    private val word = Word(Dictionary.BelRus, 27890, "бурштын", "б", "бурштын", null)
    private val entry = TranslationEntry(stress = "буршты́н", html = "<b>янтарь</b>", redirectTo = null)
    private val failing = RemoteTranslationSource { _, _ -> throw IOException("offline") }

    private fun useCase(
        local: LocalTranslationSource = LocalTranslationSource { _, _ -> null },
        api: RemoteTranslationSource = failing,
        cloud: RemoteTranslationSource = failing
    ) = GetTranslationUseCase(local, api, cloud)

    @Test
    fun `the local store answers first`() = runTest {
        val result = useCase(local = { _, _ -> entry }, api = { _, _ -> error("not called") })(word)

        val expected = Translation(word, "буршты́н", "<b>янтарь</b>", Translation.Source.Local)
        assertEquals(TranslationResult.Found(expected), result)
    }

    @Test
    fun `the admin API answers when the word is not stored`() = runTest {
        var asked: Pair<Dictionary, Long>? = null
        val result = useCase(api = { dictionary, id -> entry.also { asked = dictionary to id } })(word)

        assertEquals(Dictionary.BelRus to 27890L, asked)

        assertEquals(Translation.Source.Api, (result as TranslationResult.Found).translation.source)
    }

    @Test
    fun `Supabase answers when the API fails`() = runTest {
        val result = useCase(cloud = { _, _ -> entry })(word)

        assertEquals(Translation.Source.Cloud, (result as TranslationResult.Found).translation.source)
    }

    @Test
    fun `an empty API entry falls through to Supabase`() = runTest {
        val result = useCase(api = { _, _ -> entry.copy(html = "") }, cloud = { _, _ -> entry })(word)

        assertEquals(Translation.Source.Cloud, (result as TranslationResult.Found).translation.source)
    }

    @Test
    fun `fails when every source fails`() = runTest {
        assertFailsWith<IOException> { useCase()(word) }
    }

    @Test
    fun `a redirect ends the chain`() = runTest {
        val result = useCase(local = { _, _ -> TranslationEntry(null, null, "/tsbm/3528") })(word)

        assertEquals(TranslationResult.Redirect(WordLink(Dictionary.Tsbm, 3528)), result)
    }

    @Test
    fun `a blank redirect and blank stress are ignored`() = runTest {
        val result = useCase(api = { _, _ -> TranslationEntry(" ", "<b>янтарь</b>", "") })(word)

        val translation = (result as TranslationResult.Found).translation
        assertEquals("бурштын", translation.title)
    }

    @Test
    fun `the share link points at skarnik app`() {
        val translation = Translation(word, null, "x", Translation.Source.Api)

        assertEquals("https://skarnik.app/r/belrus/27890", translation.shareUrl)
    }
}
