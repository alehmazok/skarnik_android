package by.mazokaleh.skarnik.core.model

import kotlin.test.assertEquals
import kotlin.test.assertNull
import org.junit.Test

class DictionaryTest {
    @Test
    fun `langIds match the values stored by the Flutter app`() {
        assertEquals(Dictionary.RusBel, Dictionary.byLangId(0))
        assertEquals(Dictionary.BelRus, Dictionary.byLangId(1))
        assertEquals(Dictionary.Tsbm, Dictionary.byLangId(2))
        assertNull(Dictionary.byLangId(3))
    }

    @Test
    fun `paths match share links`() {
        assertEquals(Dictionary.BelRus, Dictionary.byPath("belrus"))
        assertEquals(Dictionary.RusBel, Dictionary.byPath("rusbel"))
        assertEquals(Dictionary.Tsbm, Dictionary.byPath("tsbm"))
        assertNull(Dictionary.byPath("be-ru"))
    }
}
