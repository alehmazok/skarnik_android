package by.mazokaleh.skarnik.core.model

/**
 * A link to a word: `skarnik.by/<dict>/<id>`, `skarnik.app/r/<dict>/<id>` (http or https, optional www) or the
 * same path without a host, as `redirect_to` and links inside translations use. Ported from Flutter's
 * `HandleAppLinkUseCase`.
 */
data class WordLink(val dictionary: Dictionary, val wordId: Long) {
    companion object {
        private val PATTERN = Regex(
            """^(https?://(www\.)?(skarnik\.by|skarnik\.app))?/(r/)?(belrus|rusbel|tsbm)/([0-9]+)/?([?#].*)?$"""
        )

        fun parse(link: String): WordLink? {
            val match = PATTERN.matchEntire(link.trim()) ?: return null
            val (_, _, _, _, path, id) = match.destructured
            val dictionary = Dictionary.byPath(path) ?: return null
            val wordId = id.toLongOrNull() ?: return null
            return WordLink(dictionary, wordId)
        }
    }
}
