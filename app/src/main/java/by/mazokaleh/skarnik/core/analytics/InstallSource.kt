package by.mazokaleh.skarnik.core.analytics

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import by.mazokaleh.skarnik.BuildConfig
import by.mazokaleh.skarnik.core.common.IoDispatcher
import by.mazokaleh.skarnik.core.crash.logError
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext

/**
 * The store the app was installed from, reported as the `install_source` user property so GA4 can count users per
 * store (shared spec with the iOS app). Values come from a fixed vocabulary; an installer we don't know is sent as its
 * package name, so new stores show up in reports.
 */
object InstallSource {
    const val USER_PROPERTY = "install_source"

    const val DEBUG = "debug"
    const val UNKNOWN = "unknown"
    const val SIDELOAD = "sideload"

    /** Firebase's limit for a user property value. */
    private const val MAX_VALUE_LENGTH = 36

    private val KNOWN_INSTALLERS = mapOf(
        "com.android.vending" to "google_play",
        "com.huawei.appmarket" to "huawei_appgallery",
        "com.hihonor.appmarket" to "honor_appmarket",
        "com.sec.android.app.samsungapps" to "samsung_galaxy_store",
        "com.xiaomi.mipicks" to "xiaomi_getapps",
        "com.xiaomi.market" to "xiaomi_getapps",
        "ru.vk.store" to "rustore",
        "com.amazon.venezia" to "amazon_appstore",
        "com.sec.android.easyMover" to "device_restore",
        "com.hicloud.android.clone" to "device_restore",
        "com.huawei.KoBackup" to "device_restore",
        "com.google.android.packageinstaller" to SIDELOAD,
        "com.android.packageinstaller" to SIDELOAD,
        "com.android.shell" to SIDELOAD
    )

    /** Maps the installing package to a report value. No installer recorded means an APK file or ADB. */
    fun fromInstaller(installer: String?): String = when {
        installer.isNullOrBlank() -> SIDELOAD
        else -> KNOWN_INSTALLERS[installer] ?: installer.take(MAX_VALUE_LENGTH)
    }
}

interface InstallSourceReader {
    /** The [InstallSource] value for this install. Never throws; [InstallSource.UNKNOWN] when the lookup fails. */
    suspend fun read(): String
}

/** Reads the installing package from [PackageManager]. No permission is needed for the app's own package. */
@Singleton
class PackageManagerInstallSource @Inject constructor(
    @ApplicationContext private val context: Context,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : InstallSourceReader {
    // The installer does not change while the process lives: a reinstall restarts it.
    @Volatile
    private var cached: String? = null

    // Debug builds skip the lookup. Otherwise it is a binder call into the system server, so off the main thread.
    override suspend fun read(): String = if (BuildConfig.DEBUG) {
        InstallSource.DEBUG
    } else {
        cached ?: withContext(ioDispatcher) { lookUp() }.also { cached = it }
    }

    private fun lookUp(): String = try {
        InstallSource.fromInstaller(installingPackage())
    } catch (e: PackageManager.NameNotFoundException) {
        unknown(e)
    } catch (e: SecurityException) {
        unknown(e)
    } catch (e: IllegalArgumentException) {
        // getInstallerPackageName's way of saying the package is not installed.
        unknown(e)
    }

    private fun installingPackage(): String? {
        val packageManager = context.packageManager
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // The store that installed the APK, not the one that started or referred the install.
            packageManager.getInstallSourceInfo(context.packageName).installingPackageName
        } else {
            @Suppress("DEPRECATION")
            packageManager.getInstallerPackageName(context.packageName)
        }
    }

    private fun unknown(error: Throwable): String {
        logError(TAG, "Cannot read the install source", error)
        return InstallSource.UNKNOWN
    }

    private companion object {
        const val TAG = "InstallSource"
    }
}

@Module
@InstallIn(SingletonComponent::class)
interface InstallSourceModule {
    @Binds
    fun bindInstallSourceReader(impl: PackageManagerInstallSource): InstallSourceReader
}
