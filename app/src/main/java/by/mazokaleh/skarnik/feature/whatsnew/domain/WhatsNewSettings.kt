package by.mazokaleh.skarnik.feature.whatsnew.domain

/** The last version whose notes were shown, or which was installed fresh. */
interface WhatsNewSettings {
    suspend fun lastSeenVersion(): String?

    suspend fun setLastSeenVersion(version: String)
}

/** What the system knows about this install. */
interface InstallInfo {
    /** `versionName` of the running app. */
    val currentVersion: String

    /**
     * True when the app was updated at least once since it was installed, including the update from the
     * Flutter app. False for a fresh install.
     */
    suspend fun wasUpdated(): Boolean
}
