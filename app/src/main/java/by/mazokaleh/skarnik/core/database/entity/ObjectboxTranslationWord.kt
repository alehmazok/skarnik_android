package by.mazokaleh.skarnik.core.database.entity

import io.objectbox.annotation.ConflictStrategy
import io.objectbox.annotation.Entity
import io.objectbox.annotation.Id
import io.objectbox.annotation.Index
import io.objectbox.annotation.Uid
import io.objectbox.annotation.Unique

/**
 * A cached or downloaded translation (`objectbox_translation` store).
 *
 * Mirrors `ObjectboxTranslationWord` in skarnik-flutter; see [ObjectboxSearchWord] for the compatibility rules.
 * [key] is `"$langId:$wordId"`: `wordId` alone is not unique across dictionaries, and replace-on-conflict
 * lets re-downloading a word overwrite it in place.
 */
@Entity
@Uid(4)
data class ObjectboxTranslationWord(
    @Id var id: Long = 0,
    @Index var langId: Long = 0,
    var wordId: Long = 0,
    @Unique(onConflict = ConflictStrategy.REPLACE) var key: String = "",
    var stress: String? = null,
    var translation: String = "",
    var redirectTo: String? = null
) {
    companion object {
        fun keyOf(langId: Long, wordId: Long): String = "$langId:$wordId"
    }
}
