package by.mazokaleh.skarnik.feature.translation.domain

import by.mazokaleh.skarnik.core.model.Dictionary

/** A dictionary entry as every source returns it: either a translation or a redirect to another word. */
data class TranslationEntry(val stress: String?, val html: String?, val redirectTo: String?)

/** Translations stored on the device: offline dictionaries downloaded from settings. */
fun interface LocalTranslationSource {
    /** Null when the word is not stored. */
    suspend fun find(dictionary: Dictionary, wordId: Long): TranslationEntry?
}

/** A network source. Throws when the word cannot be fetched or does not exist there. */
fun interface RemoteTranslationSource {
    suspend fun fetch(dictionary: Dictionary, wordId: Long): TranslationEntry
}
