package by.mazokaleh.skarnik.feature.favorites.data

import androidx.datastore.core.DataMigration
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import by.mazokaleh.skarnik.core.datastore.FlutterPreferences
import by.mazokaleh.skarnik.core.datastore.FlutterPreferencesMigration
import by.mazokaleh.skarnik.feature.favorites.domain.BookmarksSortOrder
import by.mazokaleh.skarnik.feature.favorites.domain.BookmarksSortRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

/** Flutter's `SharedPreferencesFavoritesSortRepository`, with the same key and values. */
class DataStoreBookmarksSortRepository @Inject constructor(
    private val settings: DataStore<Preferences>
) : BookmarksSortRepository {
    override val sortOrder: Flow<BookmarksSortOrder> = settings.data
        .map { BookmarksSortOrder.byKey(it[SORT_ORDER]) }
        .distinctUntilChanged()

    override suspend fun setSortOrder(sortOrder: BookmarksSortOrder) {
        settings.edit { it[SORT_ORDER] = sortOrder.key }
    }

    companion object {
        val SORT_ORDER = stringPreferencesKey("favorites_sort_order")
    }
}

@Module
@InstallIn(SingletonComponent::class)
interface BookmarksSortModule {
    @Binds
    fun bindBookmarksSortRepository(impl: DataStoreBookmarksSortRepository): BookmarksSortRepository
}

@Module
@InstallIn(SingletonComponent::class)
object BookmarksSortMigrationModule {
    @Provides
    @IntoSet
    fun provideSortOrderMigration(
        @FlutterPreferences flutterPreferences: DataStore<Preferences>
    ): DataMigration<Preferences> = bookmarksSortMigration(flutterPreferences)
}

/** Copies Flutter's `favorites_sort_order` into the app's settings. */
fun bookmarksSortMigration(flutterPreferences: DataStore<Preferences>): DataMigration<Preferences> {
    val key = DataStoreBookmarksSortRepository.SORT_ORDER
    return FlutterPreferencesMigration(flutterPreferences, name = key.name) { from, to ->
        from[key]?.let { to[key] = it }
    }
}
