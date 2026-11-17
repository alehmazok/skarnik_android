package by.mazokaleh.skarnik.core.model

/**
 * A link to a word: `skarnik.app/r/<dict>/<id>` (http or https, optional www), `/<dict>/<id>` on any host (the
 * legacy links inside translation HTML) or either path without a host, as `redirect_to` uses. Ported from
 * Flutter's `HandleAppLinkUseCase`. Which hosts open the app from outside is up to the manifest's VIEW filters.
 */
data class WordLink(val dictionary: Dictionary, val wordId: Long) {
    companion object {
        private val PATTERN = Regex(
            """^(https?://[^/?#]+)?/(r/)?""" +
                """(belrus|rusbel|tsbm)/([0-9]+)/?([?#].*)?$"""
        )

        private const val DICTIONARY_GROUP = 3
        private const val ID_GROUP = 4

        fun parse(link: String): WordLink? {
            // Indexed groups: named-group lookup needs API 26 (minSdk is 24).
            val groups = PATTERN.matchEntire(link.trim())?.groupValues ?: return null
            val dictionary = Dictionary.byPath(groups[DICTIONARY_GROUP])
            val wordId = groups[ID_GROUP].toLongOrNull()
            return if (dictionary != null && wordId != null) WordLink(dictionary, wordId) else null
        }
    }
}
