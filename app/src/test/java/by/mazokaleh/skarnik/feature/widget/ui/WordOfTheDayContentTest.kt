package by.mazokaleh.skarnik.feature.widget.ui

import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.glance.action.action
import androidx.glance.appwidget.testing.unit.runGlanceAppWidgetUnitTest
import androidx.glance.testing.unit.hasText
import by.mazokaleh.skarnik.feature.widget.domain.WordOfTheDay
import org.junit.Test

/** The widget's content, rendered by Glance's unit test host (no device). */
class WordOfTheDayContentTest {
    private val word = WordOfTheDay(27890, "бурштын", "янтарь\nсмала")

    @Test
    fun `shows the title, the word and its translation`() = runGlanceAppWidgetUnitTest {
        setAppWidgetSize(DpSize(250.dp, 160.dp))
        provideComposable {
            WordOfTheDayContent(title = "Слова дня", word = word, onClick = action {})
        }

        onNode(hasText("Слова дня")).assertExists()
        onNode(hasText("бурштын")).assertExists()
        onNode(hasText("янтарь\nсмала")).assertExists()
    }
}
