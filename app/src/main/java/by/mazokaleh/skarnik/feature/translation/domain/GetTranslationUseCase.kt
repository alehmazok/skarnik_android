package by.mazokaleh.skarnik.feature.translation.domain

import android.util.Log
import by.mazokaleh.skarnik.core.common.suspendRunCatching
import by.mazokaleh.skarnik.core.model.Word
import by.mazokaleh.skarnik.core.model.WordLink
import javax.inject.Inject
import javax.inject.Qualifier

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class AdminApi

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class Cloud

sealed interface TranslationResult {
    data class Found(val translation: Translation) : TranslationResult

    /** The entry is an alias: open [target] instead. */
    data class Redirect(val target: WordLink) : TranslationResult
}

/**
 * Ported from Flutter's `GetTranslationUseCase`: the local store (offline dictionaries), then the admin API,
 * then Supabase. The skarnik.by page scrape that came last in Flutter is dropped (parity 2.2).
 * A source that answers with a redirect ends the chain. Throws when every source fails.
 */
class GetTranslationUseCase @Inject constructor(
    private val local: LocalTranslationSource,
    @AdminApi private val api: RemoteTranslationSource,
    @Cloud private val cloud: RemoteTranslationSource
) {
    suspend operator fun invoke(word: Word): TranslationResult {
        val stored = local.find(word.dictionary, word.wordId)
        if (stored != null) return stored.toResult(word, Translation.Source.Local)
        // A malformed API entry falls through to Supabase too, as in Flutter.
        return suspendRunCatching { api.fetch(word.dictionary, word.wordId).toResult(word, Translation.Source.Api) }
            .getOrElse { error ->
                Log.w(TAG, "Admin API failed for ${word.dictionary.path}/${word.wordId}", error)
                cloud.fetch(word.dictionary, word.wordId).toResult(word, Translation.Source.Cloud)
            }
    }

    private fun TranslationEntry.toResult(word: Word, source: Translation.Source): TranslationResult {
        val redirect = redirectTo?.takeIf { it.isNotBlank() }
        if (redirect != null) {
            val target = WordLink.parse(redirect) ?: throw InvalidEntryException("Unknown redirect '$redirect'")
            return TranslationResult.Redirect(target)
        }
        val html = html?.takeIf { it.isNotBlank() } ?: throw InvalidEntryException("Empty translation")
        return TranslationResult.Found(Translation(word, stress?.takeIf { it.isNotBlank() }, html, source))
    }

    private companion object {
        const val TAG = "GetTranslation"
    }
}

class InvalidEntryException(message: String) : IllegalStateException(message)
