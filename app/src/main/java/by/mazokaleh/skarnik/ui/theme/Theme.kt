package by.mazokaleh.skarnik.ui.theme

import android.os.Build
import androidx.annotation.ChecksSdkIntAtLeast
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import by.mazokaleh.skarnik.core.settings.ThemeMode

// Brand palette, shared with skarnik.app.
private val BrandRedLight = Color(0xFFE1251B)
private val BrandRedDark = Color(0xFFFF4438)
private val BrandDarkBackground = Color(0xFF0F0F10)

internal val BrandLightColors = lightColorScheme(
    primary = BrandRedLight,
    onPrimary = Color.White,
    background = Color.White,
    surface = Color.White
)

internal val BrandDarkColors = darkColorScheme(
    primary = BrandRedDark,
    onPrimary = Color.White,
    background = BrandDarkBackground,
    surface = BrandDarkBackground
)

/**
 * App theme. Follows the system light/dark setting and uses Material You dynamic colour on Android 12+
 * by default; [useDynamicColor] = false (or older Android) switches to the brand palette.
 */
@Composable
fun SkarnikTheme(
    themeMode: ThemeMode = ThemeMode.System,
    useDynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val darkTheme = themeMode.isDark()
    val colorScheme = when {
        useDynamicColor && isDynamicColorAvailable -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> BrandDarkColors
        else -> BrandLightColors
    }
    MaterialTheme(colorScheme = colorScheme, content = content)
}

/** Whether [this] mode shows the dark theme right now. */
@Composable
fun ThemeMode.isDark(): Boolean = when (this) {
    ThemeMode.System -> isSystemInDarkTheme()
    ThemeMode.Light -> false
    ThemeMode.Dark -> true
}

/** Material You is available from Android 12. */
@get:ChecksSdkIntAtLeast(api = Build.VERSION_CODES.S)
val isDynamicColorAvailable: Boolean get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
