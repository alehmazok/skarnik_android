package by.mazokaleh.skarnik.feature.translation.domain

import kotlin.test.assertEquals
import kotlin.test.assertNull
import org.junit.Test

class AbbreviationsTest {
    private fun found(text: String) = Abbreviations.findIn(text).map { it.abbreviation to it.expansion }

    @Test
    fun `expands abbreviations from the list and the forms Bel-Rus uses`() {
        assertEquals("разговорное слово, выражение", Abbreviations.expansionOf("разг."))
        assertEquals("женский род", Abbreviations.expansionOf("жен."))
        assertEquals("мужской род", Abbreviations.expansionOf("муж."))
        assertEquals("несовершенный вид", Abbreviations.expansionOf("несовер."))
        assertEquals("антропология", Abbreviations.expansionOf("антроп."))
        assertNull(Abbreviations.expansionOf("слова"))
    }

    @Test
    fun `finds several abbreviations in one label`() {
        assertEquals(
            listOf("жен." to "женский род", "мн." to "множественное число"),
            found("жен., мн. нет")
        )
        assertEquals(
            listOf("перен." to "переносное значение", "разг." to "разговорное слово, выражение"),
            found("перен. разг.")
        )
    }

    @Test
    fun `prefers the longest abbreviation`() {
        assertEquals(
            listOf("совер." to "совершенный вид", "в разн. знач." to "в разных значениях"),
            found("совер. в разн. знач.")
        )
        assertEquals(listOf("в знач. сказ." to "в значении сказуемого"), found("в знач. сказ."))
        assertEquals(listOf("ж.-д." to "железнодорожный термин"), found("ж.-д."))
    }

    @Test
    fun `a space after a dot is optional, also a non-breaking one`() {
        assertEquals(listOf("и т.п." to "и тому подобное"), found("и т.п."))
        assertEquals(listOf("и т. д." to "и так далее"), found("и т. д."))
    }

    @Test
    fun `never matches inside a word`() {
        assertEquals(emptyList(), found("ср"))
        assertEquals(emptyList(), found("устарэлае слова, устарэлы выраз"))
        assertEquals(listOf("муж." to "мужской род"), found("муж."))
        assertEquals(emptyList(), found("радиола"))
    }

    @Test
    fun `finds abbreviations without a dot as whole words`() {
        assertEquals(listOf("радио" to "радиотехника"), found("радио"))
        assertEquals(listOf("п-в" to "полуостров"), found("п-в"))
    }
}
