package by.mazokaleh.skarnik.feature.update.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import by.mazokaleh.skarnik.feature.update.domain.AppUpdateSettings
import javax.inject.Inject
import kotlinx.coroutines.flow.first

/** A native key: Flutter had no in-app updates, so there is nothing to migrate. */
class DataStoreAppUpdateSettings @Inject constructor(private val settings: DataStore<Preferences>) :
    AppUpdateSettings {
    override suspend fun promptedVersionCode(): Long? = settings.data.first()[PROMPTED_VERSION_CODE]

    override suspend fun setPromptedVersionCode(versionCode: Long) {
        settings.edit { it[PROMPTED_VERSION_CODE] = versionCode }
    }

    companion object {
        val PROMPTED_VERSION_CODE = longPreferencesKey("app_update_prompted_version_code")
    }
}
