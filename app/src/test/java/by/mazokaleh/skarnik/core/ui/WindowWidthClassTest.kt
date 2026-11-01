package by.mazokaleh.skarnik.core.ui

import androidx.compose.ui.unit.dp
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class WindowWidthClassTest {
    @Test
    fun `phones are compact, small tablets and unfolded foldables medium, large tablets expanded`() {
        assertEquals(WindowWidthClass.Compact, WindowWidthClass.of(360.dp))
        assertEquals(WindowWidthClass.Compact, WindowWidthClass.of(599.9.dp))
        assertEquals(WindowWidthClass.Medium, WindowWidthClass.of(600.dp))
        assertEquals(WindowWidthClass.Medium, WindowWidthClass.of(839.dp))
        assertEquals(WindowWidthClass.Expanded, WindowWidthClass.of(840.dp))
        assertEquals(WindowWidthClass.Expanded, WindowWidthClass.of(1280.dp))
    }

    @Test
    fun `the rail comes at medium width, the list beside the translation only at expanded`() {
        assertFalse(WindowWidthClass.Compact.hasNavigationRail)
        assertFalse(WindowWidthClass.Compact.hasListDetail)
        assertTrue(WindowWidthClass.Medium.hasNavigationRail)
        assertFalse(WindowWidthClass.Medium.hasListDetail)
        assertTrue(WindowWidthClass.Expanded.hasNavigationRail)
        assertTrue(WindowWidthClass.Expanded.hasListDetail)
    }
}
