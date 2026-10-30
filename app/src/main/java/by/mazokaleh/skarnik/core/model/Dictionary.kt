package by.mazokaleh.skarnik.core.model

/**
 * The three dictionaries. [langId] is the value stored in ObjectBox and must never change;
 * [path] is used in API URLs and share links (`skarnik.app/r/<path>/<id>`).
 */
enum class Dictionary(val langId: Long, val path: String) {
    BelRus(langId = 1, path = "belrus"),
    RusBel(langId = 0, path = "rusbel"),
    Tsbm(langId = 2, path = "tsbm");

    companion object {
        fun byLangId(langId: Long): Dictionary? = entries.firstOrNull { it.langId == langId }

        fun byPath(path: String): Dictionary? = entries.firstOrNull { it.path == path }
    }
}
