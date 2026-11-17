package by.mazokaleh.skarnik.core.model

import kotlin.test.assertEquals
import kotlin.test.assertNull
import org.junit.Test

class WordLinkTest {
    @Test
    fun `parses skarnik app, legacy translation links and bare paths`() {
        val expected = WordLink(Dictionary.RusBel, 47880)
        listOf(
            "http://www.example.org/rusbel/47880",
            "https://example.org/rusbel/47880/",
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
            "http://example.org/unknown/1",
            "http://example.org/a/rusbel/1",
            "/rusbel/abc",
            ""
        ).forEach { assertNull(WordLink.parse(it), it) }
    }
}
