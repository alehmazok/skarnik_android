package by.mazokaleh.skarnik.core.database.entity

import io.objectbox.annotation.Entity
import io.objectbox.annotation.Id
import io.objectbox.annotation.Index
import io.objectbox.annotation.IndexType
import io.objectbox.annotation.Uid
import io.objectbox.annotation.Unique

/**
 * A word in the bundled search index (`objectbox_search` store).
 *
 * Mirrors `ObjectboxSearchWord` in skarnik-flutter. Class name, property names, types, index types and
 * the UIDs in `objectbox-models/default.json` must stay identical to the Flutter model, or existing
 * installs lose their data on upgrade. See docs/adr/0001-objectbox-on-flutter-stores.md.
 */
@Entity
@Uid(1)
data class ObjectboxSearchWord(
    @Id var id: Long = 0,
    var langId: Long = 0,
    @Index(type = IndexType.VALUE) var letter: String = "",
    @Unique var wordId: Long = 0,
    var word: String = "",
    @Index(type = IndexType.VALUE) var lword: String = "",
    @Index(type = IndexType.VALUE) var lwordMask: String? = null,
)
