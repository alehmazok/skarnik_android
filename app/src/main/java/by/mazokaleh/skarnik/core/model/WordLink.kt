package by.mazokaleh.skarnik.core.model

/**
 * A link to a word: `skarnik.by/<dict>/<id>`, `skarnik.app/r/<dict>/<id>` (http or https, optional www) or the
 * same path without a host, as `redirect_to` and links inside translations use. Ported from Flutter's
 * `HandleAppLinkUseCase`.
 */
data class WordLink(val dictionary: Dictionary, val wordId: Long) {
    companion object {
        private val PATTERN = Regex(
            """^(https?://(www\.)?(skarnik\.by|skarnik\.app))?/(r/)?""" +
                """(belrus|rusbel|tsbm)/([0-9]+)/?([?#].*)?$"""
        )

        private const val DICTIONARY_GROUP = 5
        private const val ID_GROUP = 6

        fun parse(link: String): WordLink? {
            // Indexed groups: named-group lookup needs API 26 (minSdk is 24).
            val groups = PATTERN.matchEntire(link.trim())?.groupValues ?: return null
            val dictionary = Dictionary.byPath(groups[DICTIONARY_GROUP])
            val wordId = groups[ID_GROUP].toLongOrNull()
            return if (dictionary != null && wordId != null) WordLink(dictionary, wordId) else null
        }
    }
}
