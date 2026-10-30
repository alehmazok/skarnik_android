package by.mazokaleh.skarnik.feature.translation.data

import by.mazokaleh.skarnik.BuildConfig
import by.mazokaleh.skarnik.feature.translation.domain.AdminApi
import by.mazokaleh.skarnik.feature.translation.domain.Cloud
import by.mazokaleh.skarnik.feature.translation.domain.LocalTranslationSource
import by.mazokaleh.skarnik.feature.translation.domain.RemoteTranslationSource
import by.mazokaleh.skarnik.feature.translation.domain.WordRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient

@Module
@InstallIn(SingletonComponent::class)
interface TranslationModule {
    @Binds
    fun bindLocalTranslationSource(impl: ObjectBoxLocalTranslationSource): LocalTranslationSource

    @Binds
    fun bindWordRepository(impl: ObjectBoxWordRepository): WordRepository

    companion object {
        @Provides
        @AdminApi
        fun provideAdminApi(client: OkHttpClient, json: Json): RemoteTranslationSource =
            AdminApiTranslationSource(client, json, BuildConfig.API_BASE_URL.toBaseUrl())

        @Provides
        @Cloud
        fun provideSupabase(client: OkHttpClient, json: Json): RemoteTranslationSource =
            SupabaseTranslationSource(client, json, BuildConfig.SUPABASE_URL.toBaseUrl(), BuildConfig.SUPABASE_KEY)
    }
}
