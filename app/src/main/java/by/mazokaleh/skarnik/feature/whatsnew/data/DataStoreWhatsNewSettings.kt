package by.mazokaleh.skarnik.feature.whatsnew.data

import android.content.Context
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Build
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import by.mazokaleh.skarnik.BuildConfig
import by.mazokaleh.skarnik.core.common.IoDispatcher
import by.mazokaleh.skarnik.feature.whatsnew.domain.InstallInfo
import by.mazokaleh.skarnik.feature.whatsnew.domain.WhatsNewSettings
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Inject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

/** A native key: Flutter had no "what's new", so there is nothing to migrate. */
class DataStoreWhatsNewSettings @Inject constructor(private val settings: DataStore<Preferences>) :
    WhatsNewSettings {
    override suspend fun lastSeenVersion(): String? = settings.data.first()[LAST_SEEN_VERSION]

    override suspend fun setLastSeenVersion(version: String) {
        settings.edit { it[LAST_SEEN_VERSION] = version }
    }

    companion object {
        val LAST_SEEN_VERSION = stringPreferencesKey("whats_new_last_seen_version")
    }
}

/**
 * An update (from the Flutter app too) keeps the first install time and moves the last update time; a fresh
 * install has both equal. This works for people who updated before the "what's new" key existed.
 */
class PackageInstallInfo @Inject constructor(
    @ApplicationContext private val context: Context,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : InstallInfo {
    override val currentVersion: String = BuildConfig.VERSION_NAME

    override suspend fun wasUpdated(): Boolean = withContext(ioDispatcher) {
        val info = packageInfo()
        info.lastUpdateTime > info.firstInstallTime
    }

    private fun packageInfo(): PackageInfo {
        val manager = context.packageManager
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            manager.getPackageInfo(context.packageName, PackageManager.PackageInfoFlags.of(0))
        } else {
            @Suppress("DEPRECATION")
            manager.getPackageInfo(context.packageName, 0)
        }
    }
}

@Module
@InstallIn(SingletonComponent::class)
interface WhatsNewModule {
    @Binds
    fun bindWhatsNewSettings(impl: DataStoreWhatsNewSettings): WhatsNewSettings

    @Binds
    fun bindInstallInfo(impl: PackageInstallInfo): InstallInfo
}
