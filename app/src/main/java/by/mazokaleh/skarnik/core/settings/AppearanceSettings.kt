package by.mazokaleh.skarnik.core.settings

import kotlinx.coroutines.flow.Flow

/** Light or dark: the system's choice, or forced by the user. */
enum class ThemeMode(val key: String) {
    System("system"),
    Light("light"),
    Dark("dark");

    companion object {
        fun byKey(key: String?): ThemeMode = entries.firstOrNull { it.key == key } ?: System
    }
}

/** How the app looks. [dynamicColor] is Material You on Android 12+; off means the brand palette. */
data class Appearance(val themeMode: ThemeMode = ThemeMode.System, val dynamicColor: Boolean = true)

/** The appearance settings (new in the native app, so nothing to migrate from Flutter). */
interface AppearanceSettings {
    val appearance: Flow<Appearance>

    suspend fun setThemeMode(themeMode: ThemeMode)

    suspend fun setDynamicColor(enabled: Boolean)
}
