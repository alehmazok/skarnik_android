package by.mazokaleh.skarnik.feature.review.data

import androidx.datastore.core.DataMigration
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import by.mazokaleh.skarnik.core.datastore.FlutterPreferences
import by.mazokaleh.skarnik.core.datastore.FlutterPreferencesMigration
import by.mazokaleh.skarnik.feature.review.domain.ReviewSettings
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet
import javax.inject.Inject
import kotlinx.coroutines.flow.first

/** Flutter's `SharedPreferencesReviewRepository`, with the same key names. */
class DataStoreReviewSettings @Inject constructor(private val settings: DataStore<Preferences>) : ReviewSettings {
    override suspend fun isReviewRequested(): Boolean = settings.data.first()[REQUESTED] == true

    override suspend fun incrementTranslationViewCount(): Long {
        var count = 0L
        settings.edit {
            count = (it[VIEW_COUNT] ?: 0L) + 1
            it[VIEW_COUNT] = count
        }
        return count
    }

    override suspend fun markReviewRequested() {
        settings.edit { it[REQUESTED] = true }
    }

    companion object {
        val VIEW_COUNT = longPreferencesKey("review_translation_view_count")
        val REQUESTED = booleanPreferencesKey("review_already_requested")
    }
}

/**
 * Copies Flutter's view count and "already asked" flag, so people who were asked in the Flutter app are not asked
 * again. Flutter's `setInt` is stored as a long; an int is accepted too, any other type is skipped.
 */
fun reviewMigration(flutterPreferences: DataStore<Preferences>): DataMigration<Preferences> =
    FlutterPreferencesMigration(flutterPreferences, name = "review") { from, to ->
        val values = from.asMap().mapKeys { it.key.name }
        val viewCount = DataStoreReviewSettings.VIEW_COUNT
        when (val count = values[viewCount.name]) {
            is Long -> to[viewCount] = count
            is Int -> to[viewCount] = count.toLong()
        }
        val requested = DataStoreReviewSettings.REQUESTED
        (values[requested.name] as? Boolean)?.let { to[requested] = it }
    }

@Module
@InstallIn(SingletonComponent::class)
interface ReviewModule {
    @Binds
    fun bindReviewSettings(impl: DataStoreReviewSettings): ReviewSettings
}

@Module
@InstallIn(SingletonComponent::class)
object ReviewMigrationModule {
    @Provides
    @IntoSet
    fun provideReviewMigration(
        @FlutterPreferences flutterPreferences: DataStore<Preferences>
    ): DataMigration<Preferences> = reviewMigration(flutterPreferences)
}
