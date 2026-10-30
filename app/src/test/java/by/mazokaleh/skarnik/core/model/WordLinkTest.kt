package by.mazokaleh.skarnik.core.model

import kotlin.test.assertEquals
import kotlin.test.assertNull
import org.junit.Test

class WordLinkTest {
    @Test
    fun `parses skarnik by, skarnik app and bare paths`() {
        val expected = WordLink(Dictionary.RusBel, 47880)
        listOf(
            "http://www.skarnik.by/rusbel/47880",
            "https://skarnik.by/rusbel/47880/",
            "https://skarnik.app/r/rusbel/47880",
            "https://www.skarnik.app/r/rusbel/47880?utm=share",
            "/rusbel/47880",
            "/r/rusbel/47880"
        ).forEach { assertEquals(expected, WordLink.parse(it), it) }
    }

    @Test
    fun `parses every dictionary`() {
        assertEquals(WordLink(Dictionary.BelRus, 1), WordLink.parse("/belrus/1"))
        assertEquals(WordLink(Dictionary.Tsbm, 3528), WordLink.parse("/tsbm/3528"))
    }

    @Test
    fun `rejects other sites and unknown dictionaries`() {
        listOf(
            "http://by-mova.livejournal.com/1012016.html",
            "http://evil.com/rusbel/1",
            "http://www.skarnik.by/unknown/1",
            "/rusbel/abc",
            ""
        ).forEach { assertNull(WordLink.parse(it), it) }
    }
}
