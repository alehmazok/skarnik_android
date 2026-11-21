package by.mazokaleh.skarnik.feature.stress.domain

/**
 * One homonym in the picker. [ordinal] (1-based) is set only when another row of the same section would read the
 * same (no meanings, e.g. skarnik_admin's two «па́ра» nouns), so the rows stay distinguishable.
 */
data class StressPickerRow(val entry: StressWordEntry, val ordinal: Int? = null)

/** The homonyms sharing one stressed form, e.g. «па́ра». */
data class StressPickerSection(val word: String, val rows: List<StressPickerRow>) {
    /** Rows with a meaning; the «N значэнні» caption counts only these. */
    val meaningCount: Int get() = rows.count { it.entry.meaning != null }
}

/**
 * Groups stress entries by stressed form for the homonym picker (skarnik_ios `SKStressWordPicker`): sections in
 * the order their form first appears in the answer, rows in answer order.
 */
fun stressPickerSections(entries: List<StressWordEntry>): List<StressPickerSection> =
    entries.groupBy { it.word }.map { (word, group) -> StressPickerSection(word, numbered(group)) }

/**
 * What a row reads as: the meaning with its part of speech, else the part of speech, else the word. The raw
 * `tableName` stands for its label, which maps one to one.
 */
private fun StressWordEntry.readsAs(): Pair<String, String?> =
    if (meaning != null) meaning to tableName else (tableName ?: word) to null

private fun numbered(entries: List<StressWordEntry>): List<StressPickerRow> {
    val totals = entries.groupingBy { it.readsAs() }.eachCount()
    val seen = mutableMapOf<Pair<String, String?>, Int>()
    return entries.map { entry ->
        val key = entry.readsAs()
        if ((totals[key] ?: 0) > 1) {
            val ordinal = (seen[key] ?: 0) + 1
            seen[key] = ordinal
            StressPickerRow(entry, ordinal)
        } else {
            StressPickerRow(entry)
        }
    }
}
