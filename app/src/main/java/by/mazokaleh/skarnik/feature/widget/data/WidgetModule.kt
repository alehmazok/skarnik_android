package by.mazokaleh.skarnik.feature.widget.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStoreFile
import by.mazokaleh.skarnik.core.common.IoDispatcher
import by.mazokaleh.skarnik.core.datastore.FlutterPreferences
import by.mazokaleh.skarnik.feature.widget.domain.RandomWordSource
import by.mazokaleh.skarnik.feature.widget.domain.WidgetHistoryRepository
import by.mazokaleh.skarnik.feature.widget.domain.WordOfTheDayStore
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Qualifier
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob

/**
 * The widget's own DataStore (`files/datastore/word_of_the_day.preferences_pb`): its history grows with every
 * refresh, so it stays out of the app's settings file.
 */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class WidgetPreferences

@Module
@InstallIn(SingletonComponent::class)
interface WidgetModule {
    @Binds
    fun bindRandomWordSource(impl: ObjectBoxRandomWordSource): RandomWordSource

    @Binds
    fun bindHistory(impl: DataStoreWidgetHistoryRepository): WidgetHistoryRepository

    @Binds
    fun bindStore(impl: DataStoreWordOfTheDayStore): WordOfTheDayStore

    companion object {
        @Provides
        @Singleton
        @WidgetPreferences
        fun providePreferences(
            @ApplicationContext context: Context,
            @IoDispatcher ioDispatcher: CoroutineDispatcher,
            @FlutterPreferences flutterPreferences: DataStore<Preferences>
        ): DataStore<Preferences> = PreferenceDataStoreFactory.create(
            migrations = listOf(widgetHistoryMigration(flutterPreferences), homeWidgetMigration(context)),
            scope = CoroutineScope(ioDispatcher + SupervisorJob()),
            produceFile = { context.preferencesDataStoreFile("word_of_the_day") }
        )
    }
}
