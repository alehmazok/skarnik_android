package by.mazokaleh.skarnik.feature.stress.ui

import by.mazokaleh.skarnik.R
import kotlin.test.assertEquals
import kotlin.test.assertNull
import org.junit.Test

class PartOfSpeechTest {
    @Test
    fun `every starnik table name has a Belarusian label`() {
        val expected = mapOf(
            "Adjectives" to R.string.stress_part_adjectives,
            "Adverbs" to R.string.stress_part_adverbs,
            "Conjunctions" to R.string.stress_part_conjunctions,
            "Interjections" to R.string.stress_part_interjections,
            "Nouns" to R.string.stress_part_nouns,
            "Numerals" to R.string.stress_part_numerals,
            "Particles" to R.string.stress_part_particles,
            "Participles" to R.string.stress_part_participles,
            "Predicatives" to R.string.stress_part_predicatives,
            "Prepositions" to R.string.stress_part_prepositions,
            "Pronouns" to R.string.stress_part_pronouns,
            "Verbs" to R.string.stress_part_verbs,
        )
        expected.forEach { (tableName, res) -> assertEquals(res, partOfSpeechRes(tableName), tableName) }
    }

    @Test
    fun `an unknown table name has no label resource`() {
        assertNull(partOfSpeechRes("Unknown"))
    }
}
