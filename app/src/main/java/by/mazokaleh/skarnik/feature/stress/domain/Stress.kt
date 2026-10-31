package by.mazokaleh.skarnik.feature.stress.domain

import javax.inject.Qualifier

/**
 * Which backend a [StressWordEntry.id] belongs to. The two id spaces are unrelated, so the source travels with
 * the id everywhere a table is fetched.
 */
enum class StressSource {
    /** starnik.by, the primary source. */
    Starnik,

    /** skarnik_admin's `stress_words` (GrammarDB import), the fallback. */
    Admin
}

/** A dictionary form that has a stress table. [lemma] is unstressed (matched against the query), [word] stressed. */
data class StressWordEntry(
    val id: Long,
    val lemma: String,
    val word: String,
    val tableName: String?,
    val source: StressSource
)

/** One row of a stress table; both cells are HTML. */
data class StressRow(val title: String, val content: String)

/** A stress backend. Both calls throw when the backend cannot be reached or answers with something unexpected. */
interface StressBackend {
    suspend fun resolveWordList(word: String): List<StressWordEntry>

    suspend fun stressTable(id: Long): List<StressRow>
}

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class StarnikStress

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class AdminStress

sealed interface StressResult<out T> {
    data class Found<T>(val value: T) : StressResult<T>

    /** The backends answered, but have nothing for the word. */
    data object NotFound : StressResult<Nothing>

    data class Failed(val error: Throwable) : StressResult<Nothing>
}
