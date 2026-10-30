package by.mazokaleh.skarnik.core.database.entity

import io.objectbox.annotation.Entity
import io.objectbox.annotation.Id
import io.objectbox.annotation.Uid

/**
 * A bookmarked word. Lives in the `objectbox_history` store, as in Flutter.
 *
 * Mirrors `ObjectboxFavoriteWord` in skarnik-flutter; see [ObjectboxSearchWord] for the compatibility rules.
 */
@Entity
@Uid(3)
data class ObjectboxFavoriteWord(
    @Id var id: Long = 0,
    var langId: Long = 0,
    var letter: String = "",
    var wordId: Long = 0,
    var word: String = ""
)
