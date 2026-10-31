package by.mazokaleh.skarnik.navigation

import by.mazokaleh.skarnik.core.model.Dictionary
import by.mazokaleh.skarnik.core.model.WordLink
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import org.junit.Test
import org.w3c.dom.Element

/** Keeps the manifest's VIEW filters and [WordLink]'s parser in step: every claimed URL opens a word. */
class AppLinkFiltersTest {
    private data class Filter(
        val autoVerify: Boolean,
        val schemes: List<String>,
        val hosts: List<String>,
        val prefixes: List<String>
    ) {
        /** One sample word URL per scheme, host and dictionary the filter claims (`/r/` takes any dictionary). */
        fun sampleLinks(): List<Pair<String, Dictionary>> = schemes.flatMap { scheme ->
            hosts.flatMap { host ->
                prefixes.flatMap { prefix ->
                    Dictionary.entries
                        .filter { prefix == "/r/" || prefix == "/${it.path}/" }
                        .map { "$scheme://$host$prefix${if (prefix == "/r/") "${it.path}/" else ""}123" to it }
                }
            }
        }
    }

    private val filters: List<Filter> by lazy {
        val factory = DocumentBuilderFactory.newInstance().apply { isNamespaceAware = true }
        val manifest = factory.newDocumentBuilder().parse(File("src/main/AndroidManifest.xml"))
        val nodes = manifest.getElementsByTagName("intent-filter")
        (0 until nodes.length).map { nodes.item(it) as Element }
            .filter { filter -> filter.children("action").any { it.android("name") == "android.intent.action.VIEW" } }
            .map { filter ->
                val data = filter.children("data")
                Filter(
                    autoVerify = filter.android("autoVerify") == "true",
                    schemes = data.mapNotNull { it.android("scheme") },
                    hosts = data.mapNotNull { it.android("host") },
                    prefixes = data.mapNotNull { it.android("pathPrefix") }
                )
            }
    }

    @Test
    fun `every claimed host and path opens a word`() {
        assertTrue(filters.isNotEmpty())
        filters.forEach { filter ->
            assertTrue(filter.autoVerify, "autoVerify on $filter")
            assertEquals(setOf("http", "https"), filter.schemes.toSet())
            val links = filter.sampleLinks()
            assertEquals(filter.schemes.size * filter.hosts.size * Dictionary.entries.size, links.size, "$filter")
            links.forEach { (url, dictionary) -> assertEquals(WordLink(dictionary, 123), WordLink.parse(url), url) }
        }
    }

    @Test
    fun `claims the share link and every legacy site dictionary`() {
        val claimed = filters.flatMap { f -> f.hosts.flatMap { h -> f.prefixes.map { "$h$it" } } }.toSet()
        Dictionary.entries.forEach {
            assertTrue("skarnik.by/${it.path}/" in claimed, it.path)
            assertTrue("www.skarnik.by/${it.path}/" in claimed, it.path)
        }
        assertTrue("skarnik.app/r/" in claimed)
        assertTrue("www.skarnik.app/r/" in claimed)
    }

    private fun Element.children(tag: String): List<Element> {
        val nodes = getElementsByTagName(tag)
        return (0 until nodes.length).map { nodes.item(it) as Element }
    }

    private fun Element.android(name: String): String? = getAttributeNS(ANDROID_NS, name).takeIf { it.isNotEmpty() }

    private companion object {
        const val ANDROID_NS = "http://schemas.android.com/apk/res/android"
    }
}
