package by.mazokaleh.skarnik.core.settings

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.edit
import java.io.File
import kotlin.test.assertEquals
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class DataStoreAppearanceSettingsTest {
    @get:Rule
    val tmp = TemporaryFolder()

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val dataStore = PreferenceDataStoreFactory.create(
        scope = scope,
        produceFile = { File(tmp.root, "datastore/settings.preferences_pb") }
    )
    private val settings = DataStoreAppearanceSettings(dataStore)

    @After
    fun tearDown() {
        scope.cancel()
    }

    @Test
    fun `defaults follow the system with Material You`() = runBlocking {
        assertEquals(Appearance(ThemeMode.System, dynamicColor = true), settings.appearance.first())
    }

    @Test
    fun `the theme and the colour switch are saved`() = runBlocking {
        settings.setThemeMode(ThemeMode.Dark)
        settings.setDynamicColor(false)

        assertEquals(Appearance(ThemeMode.Dark, dynamicColor = false), settings.appearance.first())
        assertEquals("dark", dataStore.data.first()[DataStoreAppearanceSettings.THEME_MODE])
    }

    @Test
    fun `an unknown stored theme falls back to the system`() = runBlocking {
        dataStore.edit { it[DataStoreAppearanceSettings.THEME_MODE] = "amoled" }

        assertEquals(ThemeMode.System, settings.appearance.first().themeMode)
    }
}
