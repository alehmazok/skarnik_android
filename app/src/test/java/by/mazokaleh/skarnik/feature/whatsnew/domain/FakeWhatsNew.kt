package by.mazokaleh.skarnik.feature.whatsnew.domain

class FakeWhatsNewSettings(var lastSeen: String? = null) : WhatsNewSettings {
    var failure: Exception? = null

    override suspend fun lastSeenVersion(): String? {
        failure?.let { throw it }
        return lastSeen
    }

    override suspend fun setLastSeenVersion(version: String) {
        lastSeen = version
    }
}

class FakeInstallInfo(override val currentVersion: String, var updated: Boolean) : InstallInfo {
    override suspend fun wasUpdated(): Boolean = updated
}

fun release(version: String) = WhatsNewRelease(version, listOf(WhatsNewItem(icon = 1, title = 2, text = 3)))
