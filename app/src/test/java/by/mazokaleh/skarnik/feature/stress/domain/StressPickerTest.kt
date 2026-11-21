package by.mazokaleh.skarnik.feature.stress.domain

import kotlin.test.assertEquals
import org.junit.Test

class StressPickerTest {
    private fun entry(
        id: Long,
        word: String,
        tableName: String? = "Nouns",
        meaning: String? = null,
        source: StressSource = StressSource.Starnik
    ) = StressWordEntry(id, word.replace("́", ""), word, tableName, source, meaning)

    @Test
    fun `sections group by stressed form in answer order`() {
        val sections = stressPickerSections(
            listOf(
                entry(52855, "па́ра", meaning = "два аднолькавыя прадметы"),
                entry(52857, "пара́"),
                entry(52856, "па́ра", meaning = "стан вадкасці")
            )
        )

        assertEquals(listOf("па́ра", "пара́"), sections.map { it.word })
        assertEquals(listOf(52855L, 52856L), sections[0].rows.map { it.entry.id })
        assertEquals(listOf(52857L), sections[1].rows.map { it.entry.id })
    }

    @Test
    fun `same stress everywhere gives one section`() {
        val sections = stressPickerSections(
            listOf(entry(1, "а́"), entry(234065, "а́", "Conjunctions"), entry(250353, "а́", "Prepositions"))
        )

        assertEquals(1, sections.size)
        assertEquals(listOf(null, null, null), sections[0].rows.map { it.ordinal })
    }

    @Test
    fun `identical rows are numbered within their section only`() {
        val sections = stressPickerSections(
            listOf(
                entry(476123, "па́ра", source = StressSource.Admin),
                entry(476124, "па́ра", source = StressSource.Admin),
                entry(49142, "пара́", source = StressSource.Admin)
            )
        )

        assertEquals(listOf(1, 2), sections[0].rows.map { it.ordinal })
        assertEquals(listOf(null), sections[1].rows.map { it.ordinal })
    }

    @Test
    fun `rows without a part of speech read as the word and are numbered by it`() {
        val rows = stressPickerSections(listOf(entry(1, "па́ра", null), entry(2, "па́ра", null)))[0].rows

        assertEquals(listOf(1, 2), rows.map { it.ordinal })
    }

    @Test
    fun `distinct meanings are not numbered`() {
        val rows = stressPickerSections(
            listOf(entry(1, "па́ра", meaning = "адно"), entry(2, "па́ра", meaning = "іншае"))
        )[0].rows

        assertEquals(listOf(null, null), rows.map { it.ordinal })
    }

    @Test
    fun `a meaning and a bare part of speech read differently`() {
        val rows = stressPickerSections(listOf(entry(1, "па́ра", meaning = "Nouns"), entry(2, "па́ра")))[0].rows

        assertEquals(listOf(null, null), rows.map { it.ordinal })
    }

    @Test
    fun `meaning count counts only rows with a meaning`() {
        val section = stressPickerSections(
            listOf(entry(1, "па́ра", meaning = "адно"), entry(2, "па́ра"), entry(3, "па́ра", meaning = "іншае"))
        ).single()

        assertEquals(2, section.meaningCount)
    }
}
