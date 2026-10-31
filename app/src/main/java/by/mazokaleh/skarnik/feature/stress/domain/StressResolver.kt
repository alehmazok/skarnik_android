package by.mazokaleh.skarnik.feature.stress.domain

import android.util.Log
import by.mazokaleh.skarnik.core.common.suspendRunCatching
import javax.inject.Inject

/**
 * Ported from Flutter's `ResolveStressWordListUseCase` and `GetStressTableUseCase` (spec:
 * skarnik-flutter `shared/stress_spec.md`). A strict cascade, never a merge: starnik.by's exact matches win;
 * skarnik_admin is asked only when starnik.by failed or had no exact match. A table is fetched only from the
 * source that produced the entry.
 */
class StressResolver @Inject constructor(
    @StarnikStress private val primary: StressBackend,
    @AdminStress private val fallback: StressBackend
) {
    suspend fun resolveWords(word: String): StressResult<List<StressWordEntry>> {
        suspendRunCatching { primary.exactMatches(word) }
            .onSuccess { if (it.isNotEmpty()) return StressResult.Found(it) }
            .onFailure { Log.w(TAG, "starnik.by cannot resolve «$word», trying skarnik_admin", it) }
        return suspendRunCatching { fallback.exactMatches(word) }.fold(
            onSuccess = { if (it.isEmpty()) StressResult.NotFound else StressResult.Found(it) },
            onFailure = {
                Log.e(TAG, "skarnik_admin cannot resolve «$word»", it)
                StressResult.Failed(it)
            }
        )
    }

    /** No retry on the other source: an id from one source means a different word, or nothing, in the other. */
    suspend fun table(id: Long, source: StressSource): StressResult<List<StressRow>> {
        val backend = when (source) {
            StressSource.Starnik -> primary
            StressSource.Admin -> fallback
        }
        return suspendRunCatching { backend.stressTable(id) }.fold(
            onSuccess = { if (it.isEmpty()) StressResult.NotFound else StressResult.Found(it) },
            onFailure = {
                Log.e(TAG, "Cannot load the stress table $source/$id", it)
                StressResult.Failed(it)
            }
        )
    }

    // Plain equality, case-sensitive, as in Flutter and iOS.
    private suspend fun StressBackend.exactMatches(word: String) = resolveWordList(word).filter { it.lemma == word }

    private companion object {
        const val TAG = "StressResolver"
    }
}
