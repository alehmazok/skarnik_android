package by.mazokaleh.skarnik.core.model

/**
 * A link to a word: `skarnik.by/<dict>/<id>`, `skarnik.app/r/<dict>/<id>` (http or https, optional www) or the
 * same path without a host, as `redirect_to` and links inside translations use. Ported from Flutter's
 * `HandleAppLinkUseCase`.
 */
data class WordLink(val dictionary: Dictionary, val wordId: Long) {
    companion object {
        private val PATTERN = Regex(
            """^(https?://(www\.)?(skarnik\.by|skarnik\.app))?/(r/)?(?<dict>belrus|rusbel|tsbm)/(?<id>[0-9]+)/?([?#].*)?$"""
        )

        fun parse(link: String): WordLink? {
            val groups = PATTERN.matchEntire(link.trim())?.groups ?: return null
            val dictionary = groups["dict"]?.value?.let(Dictionary::byPath)
            val wordId = groups["id"]?.value?.toLongOrNull()
            return if (dictionary != null && wordId != null) WordLink(dictionary, wordId) else null
        }
    }
}
