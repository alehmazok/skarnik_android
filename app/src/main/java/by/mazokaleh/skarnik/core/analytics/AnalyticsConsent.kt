package by.mazokaleh.skarnik.core.analytics

import androidx.datastore.core.DataMigration
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import by.mazokaleh.skarnik.core.datastore.FlutterPreferences
import by.mazokaleh.skarnik.core.datastore.FlutterPreferencesMigration
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

/** The user's answer to the consent dialog. Before an answer analytics stay off ([granted] is false). */
data class ConsentState(val answered: Boolean, val granted: Boolean)

/** Flutter's `AnalyticsConsentRepository`: the stored answer, applied to the analytics SDK. */
interface AnalyticsConsent {
    val state: Flow<ConsentState>

    /** Records an answer, from the first-launch dialog or the Settings switch, and applies it. */
    suspend fun setGranted(granted: Boolean)

    /** Applies the stored answer at app start, so the SDK matches it even if its own flag was lost. */
    suspend fun applyStored()
}

@Singleton
class DataStoreAnalyticsConsent @Inject constructor(
    private val settings: DataStore<Preferences>,
    private val analytics: Analytics
) : AnalyticsConsent {
    override val state: Flow<ConsentState> = settings.data
        .map { ConsentState(answered = it[ANSWERED] == true, granted = it[GRANTED] == true) }
        .distinctUntilChanged()

    override suspend fun setGranted(granted: Boolean) {
        settings.edit {
            it[ANSWERED] = true
            it[GRANTED] = granted
        }
        analytics.setCollectionEnabled(granted)
    }

    override suspend fun applyStored() {
        analytics.setCollectionEnabled(state.first().granted)
    }

    companion object {
        // Flutter's key names, kept in the native store too.
        val ANSWERED = booleanPreferencesKey("analytics_consent_answered")
        val GRANTED = booleanPreferencesKey("analytics_consent_granted")
    }
}

@Module
@InstallIn(SingletonComponent::class)
interface AnalyticsConsentModule {
    @Binds
    fun bindAnalyticsConsent(impl: DataStoreAnalyticsConsent): AnalyticsConsent
}

@Module
@InstallIn(SingletonComponent::class)
object AnalyticsConsentMigrationModule {
    @Provides
    @IntoSet
    fun provideConsentMigration(
        @FlutterPreferences flutterPreferences: DataStore<Preferences>
    ): DataMigration<Preferences> = analyticsConsentMigration(flutterPreferences)
}

/** Copies Flutter's answer, so people who answered in the Flutter app are not asked again. */
fun analyticsConsentMigration(flutterPreferences: DataStore<Preferences>): DataMigration<Preferences> =
    FlutterPreferencesMigration(flutterPreferences, "analytics_consent") { from, to ->
        from[DataStoreAnalyticsConsent.ANSWERED]?.let { to[DataStoreAnalyticsConsent.ANSWERED] = it }
        from[DataStoreAnalyticsConsent.GRANTED]?.let { to[DataStoreAnalyticsConsent.GRANTED] = it }
    }
