package by.mazokaleh.skarnik.core.settings

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

class DataStoreAppearanceSettings @Inject constructor(private val settings: DataStore<Preferences>) :
    AppearanceSettings {
    override val appearance: Flow<Appearance> = settings.data
        .map { Appearance(themeMode = ThemeMode.byKey(it[THEME_MODE]), dynamicColor = it[DYNAMIC_COLOR] ?: true) }
        .distinctUntilChanged()

    override suspend fun setThemeMode(themeMode: ThemeMode) {
        settings.edit { it[THEME_MODE] = themeMode.key }
    }

    override suspend fun setDynamicColor(enabled: Boolean) {
        settings.edit { it[DYNAMIC_COLOR] = enabled }
    }

    companion object {
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val DYNAMIC_COLOR = booleanPreferencesKey("dynamic_color")
    }
}

@Module
@InstallIn(SingletonComponent::class)
interface AppearanceSettingsModule {
    @Binds
    fun bindAppearanceSettings(impl: DataStoreAppearanceSettings): AppearanceSettings
}
