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

// Brand palette, shared with skarnik.app. Every role is set: roles left out of lightColorScheme()/darkColorScheme()
// fall back to the M3 baseline purple. Primary, background and surface are the brand colours; the other roles are
// HCT tones of the brand red (hue 26.7): secondary chroma 24, tertiary 32 (grammar labels in translations),
// neutral 2, neutral variant 4, at the M3 tone of each role (scripts/brand_palette.py).
private val BrandRedLight = Color(0xFFE1251B)
private val BrandRedDark = Color(0xFFFF4438)
private val BrandDarkBackground = Color(0xFF0F0F10)

internal val BrandLightColors = lightColorScheme(
    primary = BrandRedLight,
    onPrimary = Color.White,
    background = Color.White,
    surface = Color.White,
    primaryContainer = Color(0xFFFFDAD5),
    onPrimaryContainer = Color(0xFF410000),
    inversePrimary = Color(0xFFFFB4A9),
    secondary = Color(0xFF82524B),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFFFDAD5),
    onSecondaryContainer = Color(0xFF33110D),
    tertiary = Color(0xFF8B4D44),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFFFDAD5),
    onTertiaryContainer = Color(0xFF380C08),
    onBackground = Color(0xFF1E1B1A),
    onSurface = Color(0xFF1E1B1A),
    surfaceVariant = Color(0xFFEDE0DE),
    onSurfaceVariant = Color(0xFF4D4544),
    inverseSurface = Color(0xFF332F2F),
    inverseOnSurface = Color(0xFFF7EFED),
    outline = Color(0xFF7F7573),
    outlineVariant = Color(0xFFD0C4C2),
    surfaceDim = Color(0xFFE0D8D7),
    surfaceBright = Color(0xFFFFF8F7),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFFAF2F0),
    surfaceContainer = Color(0xFFF4ECEB),
    surfaceContainerHigh = Color(0xFFEFE6E5),
    surfaceContainerHighest = Color(0xFFE9E1DF)
)

internal val BrandDarkColors = darkColorScheme(
    primary = BrandRedDark,
    onPrimary = Color.White,
    background = BrandDarkBackground,
    surface = BrandDarkBackground,
    primaryContainer = Color(0xFF930002),
    onPrimaryContainer = Color(0xFFFFDAD5),
    inversePrimary = Color(0xFFC00004),
    secondary = Color(0xFFF6B8AE),
    onSecondary = Color(0xFF4C2520),
    secondaryContainer = Color(0xFF673B35),
    onSecondaryContainer = Color(0xFFFFDAD5),
    tertiary = Color(0xFFFFB4A9),
    onTertiary = Color(0xFF53211A),
    tertiaryContainer = Color(0xFF6F362E),
    onTertiaryContainer = Color(0xFFFFDAD5),
    onBackground = Color(0xFFE9E1DF),
    onSurface = Color(0xFFE9E1DF),
    surfaceVariant = Color(0xFF4D4544),
    onSurfaceVariant = Color(0xFFD0C4C2),
    inverseSurface = Color(0xFFE9E1DF),
    inverseOnSurface = Color(0xFF332F2F),
    outline = Color(0xFF998E8D),
    outlineVariant = Color(0xFF4D4544),
    surfaceDim = Color(0xFF161312),
    surfaceBright = Color(0xFF3C3838),
    surfaceContainerLowest = Color(0xFF100D0D),
    surfaceContainerLow = Color(0xFF1E1B1A),
    surfaceContainer = Color(0xFF221F1E),
    surfaceContainerHigh = Color(0xFF2D2928),
    surfaceContainerHighest = Color(0xFF383433)
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
