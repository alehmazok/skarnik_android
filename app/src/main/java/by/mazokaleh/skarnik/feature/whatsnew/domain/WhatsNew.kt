package by.mazokaleh.skarnik.feature.whatsnew.domain

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes

/** One feature in the notes: an icon, a short title and one sentence. [minSdk] hides it on older Android. */
data class WhatsNewItem(
    @DrawableRes val icon: Int,
    @StringRes val title: Int,
    @StringRes val text: Int,
    val minSdk: Int = 0
)

/** The notes for one [version] (`versionName`, e.g. "4.0.0"). */
data class WhatsNewRelease(val version: String, val items: List<WhatsNewItem>)

/** Dotted version numbers compared part by part; a missing part is 0 and a suffix (`-rc1`) is ignored. */
object AppVersions {
    fun compare(left: String, right: String): Int {
        val l = parts(left)
        val r = parts(right)
        for (index in 0 until maxOf(l.size, r.size)) {
            val diff = l.getOrElse(index) { 0 }.compareTo(r.getOrElse(index) { 0 })
            if (diff != 0) return diff
        }
        return 0
    }

    private fun parts(version: String) = version.split('.').map { it.takeWhile(Char::isDigit).toIntOrNull() ?: 0 }
}

/** Releases not newer than [currentVersion], newest first: the Settings list. */
fun List<WhatsNewRelease>.history(currentVersion: String): List<WhatsNewRelease> =
    filter { AppVersions.compare(it.version, currentVersion) <= 0 }
        .sortedWith { a, b -> AppVersions.compare(b.version, a.version) }

/** The newest release after [lastSeenVersion] (any, when null) that is not newer than [currentVersion]. */
fun List<WhatsNewRelease>.unseen(currentVersion: String, lastSeenVersion: String?): WhatsNewRelease? =
    history(currentVersion).firstOrNull {
        lastSeenVersion == null || AppVersions.compare(it.version, lastSeenVersion) > 0
    }
