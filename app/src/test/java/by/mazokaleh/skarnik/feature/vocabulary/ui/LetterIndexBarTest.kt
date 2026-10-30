package by.mazokaleh.skarnik.feature.vocabulary.ui

import kotlin.test.assertEquals
import kotlin.test.assertNull
import org.junit.Test

class LetterIndexBarTest {
    @Test
    fun `a touch maps to the letter under it`() {
        assertEquals(0, letterAt(y = 0f, height = 300, count = 30))
        assertEquals(1, letterAt(y = 15f, height = 300, count = 30))
        assertEquals(29, letterAt(y = 299f, height = 300, count = 30))
    }

    @Test
    fun `a drag past either end stays on the first or last letter`() {
        assertEquals(0, letterAt(y = -40f, height = 300, count = 30))
        assertEquals(29, letterAt(y = 900f, height = 300, count = 30))
    }

    @Test
    fun `an empty or unmeasured bar has no letter`() {
        assertNull(letterAt(y = 10f, height = 300, count = 0))
        assertNull(letterAt(y = 10f, height = 0, count = 30))
    }
}
