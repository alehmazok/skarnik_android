package by.mazokaleh.skarnik.feature.translation.domain

import kotlin.test.assertEquals
import kotlin.test.assertNull
import org.junit.Test

class AbbreviationsTest {
    private fun found(text: String) = Abbreviations.russian.findIn(text).map { it.abbreviation to it.expansion }

    @Test
    fun `expands abbreviations from the list and the forms Bel-Rus uses`() {
        assertEquals("разговорное слово, выражение", Abbreviations.russian.expansionOf("разг."))
        assertEquals("женский род", Abbreviations.russian.expansionOf("жен."))
        assertEquals("мужской род", Abbreviations.russian.expansionOf("муж."))
        assertEquals("несовершенный вид", Abbreviations.russian.expansionOf("несовер."))
        assertEquals("антропология", Abbreviations.russian.expansionOf("антроп."))
        assertNull(Abbreviations.russian.expansionOf("слова"))
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

    private fun tsbm(text: String, place: TextPlace) =
        TsbmAbbreviations.findIn(text, place).map { it.abbreviation to it.expansion }

    @Test
    fun `finds a Tsbm source as the whole span, without its dot`() {
        val colas = TsbmAbbreviations.findIn(" Колас.", TextPlace.Source).single()
        assertEquals(1..5, colas.range)
        assertEquals("Колас", colas.abbreviation)
        assertEquals(listOf("Ядвігін Ш." to "Ядвігін Ш.: Выбр. тв., 1976."), tsbm("Ядвігін Ш.", TextPlace.Source))
        assertEquals(listOf("З нар." to "з народнай творчасці"), tsbm("З нар.", TextPlace.Source))
        val kalesnik = TsbmAbbreviations.findIn("І. Калеснік.", TextPlace.Source).single()
        assertEquals("Іван Калеснік", kalesnik.expansion.substringBefore(":"))
        assertEquals(emptyList(), tsbm("Прыказка.", TextPlace.Source))
        assertEquals(emptyList(), tsbm("Колас і Купала.", TextPlace.Source))
    }

    @Test
    fun `finds Tsbm abbreviations and signs only where the dictionary uses them`() {
        assertEquals(listOf("Т" to "творны склон"), tsbm("прыназоўнік з Т.", TextPlace.Label))
        assertEquals(emptyList(), tsbm("Т", TextPlace.Plain))
        assertEquals(listOf("знач." to "значэнне"), tsbm("(у 1 знач.)", TextPlace.Plain))
        assertEquals(listOf("грэч." to "грэчаскае слова"), tsbm("[Ад грэч. palaios.]", TextPlace.Plain))
        assertEquals(listOf("//" to "адценне значэння"), tsbm("//", TextPlace.Label))
        assertEquals(emptyList(), tsbm("км/г", TextPlace.Plain))
        assertEquals(emptyList(), tsbm("Разм. жарт.", TextPlace.Quote))
    }

    @Test
    fun `finds a cut in a citation but not other dots`() {
        assertEquals(
            listOf("▪" to "знак, які аддзяляе цытаты ад рэдакцыйных прыкладаў", ".." to "скарачэнне ў цытаце"),
            tsbm("Валасы павылазілі. ▪ Карані .. павыпіналіся", TextPlace.Quote)
        )
        assertEquals(listOf(".." to "скарачэнне ў цытаце"), tsbm("запрог.. [кабылу]", TextPlace.Quote))
        assertEquals(emptyList(), tsbm("Чаму?.. Так... Ну!..", TextPlace.Quote))
    }
}
