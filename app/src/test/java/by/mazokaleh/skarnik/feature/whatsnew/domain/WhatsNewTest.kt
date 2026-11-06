package by.mazokaleh.skarnik.feature.whatsnew.domain

import by.mazokaleh.skarnik.feature.whatsnew.data.WhatsNewCatalog
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import org.junit.Test

class WhatsNewTest {
    private val releases = listOf(release("4.0.0"), release("4.2.0"), release("4.1.0"))

    @Test
    fun `compares versions by number, padding missing parts and ignoring suffixes`() {
        assertTrue(AppVersions.compare("4.10.0", "4.9.0") > 0)
        assertEquals(0, AppVersions.compare("4.1", "4.1.0"))
        assertEquals(0, AppVersions.compare("4.1.0-rc1", "4.1.0"))
        assertTrue(AppVersions.compare("3.6.0", "4.0.0") < 0)
    }

    @Test
    fun `history is newest first and leaves out versions newer than the app`() {
        assertEquals(listOf("4.1.0", "4.0.0"), releases.history("4.1.3").map { it.version })
    }

    @Test
    fun `unseen is the newest release after the last seen one`() {
        assertEquals("4.1.0", releases.unseen(currentVersion = "4.1.0", lastSeenVersion = "4.0.0")?.version)
        assertEquals("4.2.0", releases.unseen(currentVersion = "4.2.1", lastSeenVersion = "4.0.0")?.version)
        assertEquals("4.1.0", releases.unseen(currentVersion = "4.1.0", lastSeenVersion = null)?.version)
        assertNull(releases.unseen(currentVersion = "4.1.5", lastSeenVersion = "4.1.0"))
    }

    @Test
    fun `the catalog is newest first and every release has items`() {
        val versions = WhatsNewCatalog.releases.map { it.version }
        assertEquals(versions.sortedWith { a, b -> AppVersions.compare(b, a) }, versions)
        assertTrue(WhatsNewCatalog.releases.all { it.items.isNotEmpty() })
    }
}
