package by.mazokaleh.skarnik.core.datastore

import android.content.Context
import androidx.datastore.core.DataMigration
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStoreFile
import by.mazokaleh.skarnik.core.common.IoDispatcher
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.Multibinds
import javax.inject.Qualifier
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob

/** The preferences the Flutter app wrote. Read only, by the migrations that move keys into the app's store. */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class FlutterPreferences

@Module
@InstallIn(SingletonComponent::class)
interface DataStoreMigrationsModule {
    /** Each feature adds a [FlutterPreferencesMigration] for its own keys with `@IntoSet`. */
    @Multibinds
    fun migrations(): Set<DataMigration<Preferences>>
}

@Module
@InstallIn(SingletonComponent::class)
object DataStoreModule {
    /** The app's settings: `files/datastore/settings.preferences_pb`. */
    @Provides
    @Singleton
    fun provideSettings(
        @ApplicationContext context: Context,
        @IoDispatcher ioDispatcher: CoroutineDispatcher,
        migrations: Set<@JvmSuppressWildcards DataMigration<Preferences>>
    ): DataStore<Preferences> = PreferenceDataStoreFactory.create(
        migrations = migrations.toList(),
        scope = CoroutineScope(ioDispatcher + SupervisorJob()),
        produceFile = { context.preferencesDataStoreFile(SETTINGS) }
    )

    /**
     * Flutter's `SharedPreferencesAsync` is a Preferences DataStore with this name, so the file is read as one.
     * Only one DataStore may own a file in a process, hence the singleton.
     */
    @Provides
    @Singleton
    @FlutterPreferences
    fun provideFlutterPreferences(
        @ApplicationContext context: Context,
        @IoDispatcher ioDispatcher: CoroutineDispatcher
    ): DataStore<Preferences> = PreferenceDataStoreFactory.create(
        scope = CoroutineScope(ioDispatcher + SupervisorJob()),
        produceFile = { context.preferencesDataStoreFile(FLUTTER_PREFERENCES) }
    )

    private const val SETTINGS = "settings"
    private const val FLUTTER_PREFERENCES = "FlutterSharedPreferences"
}
