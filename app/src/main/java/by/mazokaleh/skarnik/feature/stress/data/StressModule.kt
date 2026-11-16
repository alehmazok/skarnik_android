package by.mazokaleh.skarnik.feature.stress.data

import by.mazokaleh.skarnik.BuildConfig
import by.mazokaleh.skarnik.feature.stress.domain.AdminStress
import by.mazokaleh.skarnik.feature.stress.domain.StarnikStress
import by.mazokaleh.skarnik.feature.stress.domain.StressBackend
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.Json
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient

@Module
@InstallIn(SingletonComponent::class)
object StressModule {
    @Provides
    @StarnikStress
    fun provideStarnik(client: OkHttpClient, json: Json): StressBackend =
        StarnikStressBackend(client, json, BuildConfig.STARNIK_BASE_URL.trimEnd('/').toHttpUrl())

    @Provides
    @AdminStress
    fun provideAdmin(client: OkHttpClient, json: Json): StressBackend =
        AdminStressBackend(client, json, BuildConfig.API_BASE_URL.trimEnd('/').toHttpUrl())
}
