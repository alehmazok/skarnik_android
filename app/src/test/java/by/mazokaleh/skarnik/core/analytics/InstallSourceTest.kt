package by.mazokaleh.skarnik.core.analytics

import kotlin.test.assertEquals
import kotlin.test.assertTrue
import org.junit.Test

class InstallSourceTest {
    @Test
    fun `known stores map to the shared vocabulary`() {
        val expected = mapOf(
            "com.android.vending" to "google_play",
            "com.huawei.appmarket" to "huawei_appgallery",
            "com.hihonor.appmarket" to "honor_appmarket",
            "com.sec.android.app.samsungapps" to "samsung_galaxy_store",
            "com.xiaomi.mipicks" to "xiaomi_getapps",
            "com.xiaomi.market" to "xiaomi_getapps",
            "ru.vk.store" to "rustore",
            "com.amazon.venezia" to "amazon_appstore"
        )

        expected.forEach { (installer, value) -> assertEquals(value, InstallSource.fromInstaller(installer)) }
    }

    @Test
    fun `phone migration tools count as a device restore`() {
        listOf("com.sec.android.easyMover", "com.hicloud.android.clone", "com.huawei.KoBackup").forEach {
            assertEquals("device_restore", InstallSource.fromInstaller(it))
        }
    }

    @Test
    fun `package installers, adb and no installer count as a sideload`() {
        listOf("com.google.android.packageinstaller", "com.android.packageinstaller", "com.android.shell", null, "")
            .forEach { assertEquals("sideload", InstallSource.fromInstaller(it)) }
    }

    @Test
    fun `an unknown installer is sent as its package name, cut to Firebase's 36 characters`() {
        assertEquals("org.fdroid.fdroid", InstallSource.fromInstaller("org.fdroid.fdroid"))

        val long = "com.example.a.very.long.store.package.name"
        val value = InstallSource.fromInstaller(long)

        assertEquals(long.take(36), value)
        assertTrue(value.length <= 36)
    }

    @Test
    fun `the property name fits Firebase's 24 characters`() {
        assertTrue(InstallSource.USER_PROPERTY.length <= 24)
    }
}
