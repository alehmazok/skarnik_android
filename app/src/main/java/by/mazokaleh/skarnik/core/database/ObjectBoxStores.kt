package by.mazokaleh.skarnik.core.database

import by.mazokaleh.skarnik.core.database.entity.ObjectboxFavoriteWord
import by.mazokaleh.skarnik.core.database.entity.ObjectboxHistoryWord
import by.mazokaleh.skarnik.core.database.entity.ObjectboxSearchWord
import by.mazokaleh.skarnik.core.database.entity.ObjectboxTranslationWord
import io.objectbox.Box
import io.objectbox.BoxStore
import io.objectbox.kotlin.boxFor
import java.io.Closeable

/**
 * The three ObjectBox stores the app has used since the Flutter version. All three share one model;
 * each entity is only ever written to its own store, as in Flutter's `ObjectboxStoreHolder`.
 */
class ObjectBoxStores(
    val search: BoxStore,
    val history: BoxStore,
    val translation: BoxStore,
) : Closeable {
    val searchBox: Box<ObjectboxSearchWord> get() = search.boxFor()
    val historyBox: Box<ObjectboxHistoryWord> get() = history.boxFor()
    val favoritesBox: Box<ObjectboxFavoriteWord> get() = history.boxFor()
    val translationBox: Box<ObjectboxTranslationWord> get() = translation.boxFor()

    override fun close() {
        search.close()
        history.close()
        translation.close()
    }
}
