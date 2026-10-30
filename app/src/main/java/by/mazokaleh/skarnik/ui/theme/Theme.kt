package by.mazokaleh.skarnik.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Flutter: ColorScheme.fromSeed(seedColor: grey, primary: red, primaryContainer: red, onPrimaryContainer: white).
private val Red = Color(0xFFF44336)

private val LightColors = lightColorScheme(
    primary = Red,
    primaryContainer = Red,
    onPrimaryContainer = Color.White,
)

private val DarkColors = darkColorScheme(
    primary = Red,
    primaryContainer = Red,
    onPrimaryContainer = Color.White,
)

@Composable
fun SkarnikTheme(darkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        content = content,
    )
}
