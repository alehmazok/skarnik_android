package by.mazokaleh.skarnik.feature.widget.ui

import android.content.Context
import android.os.Build
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.LocalSize
import androidx.glance.action.Action
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.appWidgetBackground
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Column
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.material3.ColorProviders
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import by.mazokaleh.skarnik.R
import by.mazokaleh.skarnik.core.common.withAppLocale
import by.mazokaleh.skarnik.feature.widget.domain.WordOfTheDay
import by.mazokaleh.skarnik.feature.widget.domain.WordOfTheDayStore
import by.mazokaleh.skarnik.navigation.WordLinkIntents
import by.mazokaleh.skarnik.ui.theme.BrandDarkColors
import by.mazokaleh.skarnik.ui.theme.BrandLightColors
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent

@EntryPoint
@InstallIn(SingletonComponent::class)
interface WordOfTheDayWidgetEntryPoint {
    fun store(): WordOfTheDayStore
}

/**
 * «Слова дня»: a random Бел-Рус word with its translation; a tap opens the word. Material You colours on
 * Android 12+, the brand palette below, light or dark with the system.
 */
class WordOfTheDayWidget : GlanceAppWidget() {
    override val sizeMode: SizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val store = EntryPointAccessors.fromApplication(context, WordOfTheDayWidgetEntryPoint::class.java).store()
        val word = store.current() ?: WordOfTheDay.Fallback
        val title = context.withAppLocale().getString(R.string.word_of_the_day_widget_label)
        val open = actionStartActivity(WordLinkIntents.open(context, word.link))
        provideContent {
            WidgetTheme {
                WordOfTheDayContent(title = title, word = word, onClick = open)
            }
        }
    }
}

private val BrandColors = ColorProviders(light = BrandLightColors, dark = BrandDarkColors)
private val dynamicColors: Boolean get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S

@Composable
private fun WidgetTheme(content: @Composable () -> Unit) {
    GlanceTheme(colors = if (dynamicColors) GlanceTheme.colors else BrandColors, content = content)
}

private val HeaderHeight = 16.dp
private val WordHeight = 30.dp
private val LineHeight = 20.dp
private val Padding = 16.dp

/** As many translation lines as fit under the header and the word. */
@Composable
private fun translationLines(): Int {
    val free = LocalSize.current.height - Padding * 2 - HeaderHeight - WordHeight
    return (free / LineHeight).toInt().coerceAtLeast(1)
}

@Composable
fun WordOfTheDayContent(title: String, word: WordOfTheDay, onClick: Action) {
    val colors = GlanceTheme.colors
    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .appWidgetBackground()
            // The brand palette's container roles are the M3 defaults, so it uses its surface colour.
            .background(if (dynamicColors) colors.widgetBackground else colors.surface)
            .cornerRadius(android.R.dimen.system_app_widget_background_radius)
            .padding(Padding)
            .clickable(onClick)
    ) {
        Text(
            text = title,
            maxLines = 1,
            style = TextStyle(color = colors.onSurfaceVariant, fontSize = 12.sp, fontWeight = FontWeight.Medium)
        )
        Spacer(GlanceModifier.height(4.dp))
        Text(
            text = word.word,
            maxLines = 1,
            style = TextStyle(color = colors.primary, fontSize = 22.sp, fontWeight = FontWeight.Bold)
        )
        Spacer(GlanceModifier.height(4.dp))
        Text(
            text = word.translation,
            maxLines = translationLines(),
            style = TextStyle(color = colors.onSurface, fontSize = 14.sp)
        )
    }
}
