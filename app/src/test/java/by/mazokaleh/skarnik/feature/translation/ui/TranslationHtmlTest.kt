package by.mazokaleh.skarnik.feature.translation.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import org.junit.Test

class TranslationHtmlTest {
    private val colors = TranslationColors(
        accent = Color.Red,
        label = Color.Blue,
        plain = Color.Black,
        muted = Color.Gray,
        hidden = Color.White,
        link = Color.Magenta
    )
    private val clicked = mutableListOf<String>()

    private fun render(html: String) = translationHtmlToAnnotatedString(html, colors) { clicked += it }

    private fun AnnotatedString.colorOf(part: String): Color? {
        val start = text.indexOf(part)
        return spanStyles
            .lastOrNull { it.start <= start && it.end >= start + part.length && it.item.color != Color.Unspecified }
            ?.item?.color
    }

    @Test
    fun `maps legacy colours to theme roles, with or without the hash`() {
        val text = render(
            """<font size="+2" color="#831b03">янтар</font>, <i>-ру <font color="008000">муж.</font></i> """ +
                """<font color="#5F5F5F">заст.</font> <font color="#123456">іншае</font>"""
        )

        assertEquals("янтар, -ру муж. заст. іншае", text.text)
        assertEquals(Color.Red, text.colorOf("янтар"))
        assertEquals(Color.Blue, text.colorOf("муж."))
        assertEquals(Color.Gray, text.colorOf("заст."))
        assertEquals(Color.Black, text.colorOf("іншае"))
    }

    @Test
    fun `reads colour from inline css`() {
        val text = render("""<span style="font-size: small; color: #831b03;">слова</span>""")

        assertEquals(Color.Red, text.colorOf("слова"))
    }

    @Test
    fun `bold and italic`() {
        val text = render("<b>тоўсты</b> <i>курсіў</i>")

        fun styled(part: String) =
            text.spanStyles.filter { text.text.substring(it.start, it.end) == part }.map { it.item }
        assertTrue(styled("тоўсты").any { it.fontWeight == FontWeight.Bold })
        assertTrue(styled("курсіў").any { it.fontStyle == FontStyle.Italic })
    }

    @Test
    fun `keeps non-breaking spaces, collapses other whitespace, breaks lines on br`() {
        val text = render("&nbsp;&nbsp;адзін \n\n  два<br>  тры<br/>")

        assertEquals("  адзін два\nтры", text.text)
    }

    @Test
    fun `links report their href when clicked`() {
        val text = render("""<i>смотрите</i> <a href="http://www.skarnik.by/rusbel/47880">нерв</a>""")

        val links = text.getLinkAnnotations(0, text.length)
        assertEquals(1, links.size)
        val link = links.single()
        assertEquals("нерв", text.text.substring(link.start, link.end))
        val clickable = link.item as LinkAnnotation.Clickable
        clickable.linkInteractionListener!!.onClick(clickable)
        assertEquals(listOf("http://www.skarnik.by/rusbel/47880"), clicked)
    }

    @Test
    fun `drops scripts and keeps the text of unknown tags`() {
        val text = render("<script>alert(1)</script><blink>тэкст</blink>")

        assertEquals("тэкст", text.text)
    }

    @Test
    fun `styles the stress table classes`() {
        val text = render(
            """Назоўны<br><span class="pytanne">хто? што?</span>""" +
                """<br><span class="skarot">мн.</span>&nbsp;<b> ру́кі </b>"""
        )

        assertEquals("Назоўны\nхто? што?\nмн.\u00a0 ру́кі", text.text)
        assertEquals(Color.Gray, text.colorOf("хто? што?"))
        assertEquals(Color.Blue, text.colorOf("мн."))
        fun styled(part: String) =
            text.spanStyles.filter { text.text.substring(it.start, it.end) == part }.map { it.item }
        assertTrue(styled("хто? што?").any { it.fontStyle == FontStyle.Italic })
        assertTrue(styled("мн.").any { it.fontStyle == FontStyle.Italic && it.fontWeight == FontWeight.Normal })
    }
}
