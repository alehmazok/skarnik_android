package by.mazokaleh.skarnik.feature.offline.data

import androidx.datastore.core.DataMigration
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import by.mazokaleh.skarnik.BuildConfig
import by.mazokaleh.skarnik.core.datastore.FlutterPreferences
import by.mazokaleh.skarnik.feature.offline.domain.OfflineDictionarySettings
import by.mazokaleh.skarnik.feature.offline.domain.OfflineDictionarySource
import by.mazokaleh.skarnik.feature.offline.domain.OfflineDictionaryStore
import by.mazokaleh.skarnik.feature.offline.domain.OfflineDownloads
import by.mazokaleh.skarnik.feature.translation.data.toBaseUrl
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet
import java.util.concurrent.TimeUnit
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient

@Module
@InstallIn(SingletonComponent::class)
interface OfflineModule {
    @Binds
    fun bindStore(impl: ObjectBoxOfflineDictionaryStore): OfflineDictionaryStore

    @Binds
    fun bindSettings(impl: DataStoreOfflineDictionarySettings): OfflineDictionarySettings

    @Binds
    fun bindDownloads(impl: WorkManagerOfflineDownloads): OfflineDownloads

    companion object {
        private const val PAGE_TIMEOUT_SECONDS = 120L

        /**
         * Pages are a few MB each: they skip the HTTP cache (it would only push translations out of it) and get
         * a longer timeout than single translations. The client shares the app client's connections.
         */
        @Provides
        fun provideSource(client: OkHttpClient, json: Json): OfflineDictionarySource = SupabaseOfflineDictionarySource(
            client = client.newBuilder().cache(null).callTimeout(PAGE_TIMEOUT_SECONDS, TimeUnit.SECONDS).build(),
            json = json,
            projectUrl = BuildConfig.SUPABASE_URL.toBaseUrl(),
            apiKey = BuildConfig.SUPABASE_KEY
        )

        @Provides
        @IntoSet
        fun provideMigration(
            @FlutterPreferences flutterPreferences: DataStore<Preferences>
        ): DataMigration<Preferences> = offlineDictionariesMigration(flutterPreferences)
    }
}
