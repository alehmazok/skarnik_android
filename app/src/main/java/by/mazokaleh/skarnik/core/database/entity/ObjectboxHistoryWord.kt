package by.mazokaleh.skarnik.core.database.entity

import io.objectbox.annotation.Entity
import io.objectbox.annotation.Id
import io.objectbox.annotation.Uid

/**
 * A search-history entry (`objectbox_history` store).
 *
 * Mirrors `ObjectboxHistoryWord` in skarnik-flutter; see [ObjectboxSearchWord] for the compatibility rules.
 */
@Entity
@Uid(2)
data class ObjectboxHistoryWord(
    @Id var id: Long = 0,
    var langId: Long = 0,
    var letter: String = "",
    var wordId: Long = 0,
    var word: String = "",
    var lword: String = "",
    var lwordMask: String? = null,
)
