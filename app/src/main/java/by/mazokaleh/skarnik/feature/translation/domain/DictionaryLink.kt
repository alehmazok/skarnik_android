package by.mazokaleh.skarnik.feature.translation.domain

import by.mazokaleh.skarnik.core.common.suspendRunCatching
import by.mazokaleh.skarnik.core.config.DictionaryLinkPlacement
import by.mazokaleh.skarnik.core.config.RemoteConfig
import by.mazokaleh.skarnik.core.crash.logError
import by.mazokaleh.skarnik.core.model.Dictionary
import by.mazokaleh.skarnik.core.model.Word
import by.mazokaleh.skarnik.core.model.WordLink

/** Finds an entry of the bundled index by its headword, for the link to the same word in the other dictionary. */
fun interface SameWordIndex {
    /** The [dictionary] entry whose `lword` is [lword], the lowest id when there are homonyms; null when none. */
    suspend fun sameWord(lword: String, dictionary: Dictionary): WordLink?
}

/**
 * The dictionary a translation links to for the same headword: Бел-Рус ⇄ ТСБМ, both Belarusian headwords.
 * Рус-Бел headwords are Russian, so it has none.
 */
val Dictionary.linkedDictionary: Dictionary?
    get() = when (this) {
        Dictionary.BelRus -> Dictionary.Tsbm
        Dictionary.Tsbm -> Dictionary.BelRus
        Dictionary.RusBel -> null
    }

/** The entry of the same headword in the other dictionary (Бел-Рус ⇄ ТСБМ), shown at [placement] (never `None`). */
data class DictionaryLink(val target: WordLink, val placement: DictionaryLinkPlacement)

/**
 * The link to the entry of the same headword in [linkedDictionary] (an exact `lword` match; ~70% of Бел-Рус entries
 * have one in ТСБМ, ~81% of ТСБМ entries in Бел-Рус), placed as the `dictionary_link_placement` experiment says.
 * Null for the control group (`none`, no lookup), when there is no such entry, or when the index cannot be read.
 */
class FindDictionaryLinkUseCase(private val index: SameWordIndex, private val remoteConfig: RemoteConfig) {
    suspend operator fun invoke(word: Word): DictionaryLink? {
        val dictionary = word.dictionary.linkedDictionary ?: return null
        val placement = remoteConfig.dictionaryLinkPlacement
        if (placement == DictionaryLinkPlacement.None) return null
        val target = suspendRunCatching { index.sameWord(word.lword, dictionary) }
            .getOrElse {
                logError(TAG, "Cannot look up ${word.lword} in ${dictionary.path}", it)
                null
            }
        return target?.let { DictionaryLink(it, placement) }
    }

    private companion object {
        const val TAG = "DictionaryLink"
    }
}
