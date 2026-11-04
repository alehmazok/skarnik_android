package by.mazokaleh.skarnik.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.ui.graphics.Color
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import org.junit.Test

class BrandColorsTest {
    /** Every role a screen can pick up, so a role left at the M3 baseline (purple) shows up here. */
    private fun ColorScheme.roles(): Map<String, Color> = mapOf(
        "primary" to primary, "onPrimary" to onPrimary,
        "primaryContainer" to primaryContainer, "onPrimaryContainer" to onPrimaryContainer,
        "inversePrimary" to inversePrimary, "surfaceTint" to surfaceTint,
        "secondary" to secondary, "onSecondary" to onSecondary,
        "secondaryContainer" to secondaryContainer, "onSecondaryContainer" to onSecondaryContainer,
        "tertiary" to tertiary, "onTertiary" to onTertiary,
        "tertiaryContainer" to tertiaryContainer, "onTertiaryContainer" to onTertiaryContainer,
        "background" to background, "onBackground" to onBackground,
        "surface" to surface, "onSurface" to onSurface,
        "surfaceVariant" to surfaceVariant, "onSurfaceVariant" to onSurfaceVariant,
        "inverseSurface" to inverseSurface, "inverseOnSurface" to inverseOnSurface,
        "outline" to outline, "outlineVariant" to outlineVariant,
        "surfaceDim" to surfaceDim, "surfaceBright" to surfaceBright,
        "surfaceContainerLowest" to surfaceContainerLowest, "surfaceContainerLow" to surfaceContainerLow,
        "surfaceContainer" to surfaceContainer, "surfaceContainerHigh" to surfaceContainerHigh,
        "surfaceContainerHighest" to surfaceContainerHighest
    )

    /** Hue in degrees of a colour with visible chroma, or null for greys. */
    private fun Color.hueOrNull(): Float? {
        val max = maxOf(red, green, blue)
        val min = minOf(red, green, blue)
        if (max - min < GREY_DELTA) return null
        val hue = when (max) {
            red -> (green - blue) / (max - min)
            green -> 2 + (blue - red) / (max - min)
            else -> 4 + (red - green) / (max - min)
        } * 60
        return (hue + 360) % 360
    }

    private fun assertNoPurple(scheme: ColorScheme) {
        val purple = scheme.roles().filterValues { color -> color.hueOrNull()?.let { it in PURPLE } == true }
        assertTrue(purple.isEmpty(), "Roles still on the baseline purple: ${purple.keys}")
    }

    @Test
    fun `light brand palette has no baseline purple`() = assertNoPurple(BrandLightColors)

    @Test
    fun `dark brand palette has no baseline purple`() = assertNoPurple(BrandDarkColors)

    @Test
    fun `brand colours stay as agreed`() {
        assertEquals(Color(0xFFE1251B), BrandLightColors.primary)
        assertEquals(Color.White, BrandLightColors.background)
        assertEquals(Color.White, BrandLightColors.surface)
        assertEquals(Color(0xFFFF4438), BrandDarkColors.primary)
        assertEquals(Color(0xFF0F0F10), BrandDarkColors.background)
        assertEquals(Color(0xFF0F0F10), BrandDarkColors.surface)
    }

    private companion object {
        const val GREY_DELTA = 0.02f
        val PURPLE = 230f..345f
    }
}
