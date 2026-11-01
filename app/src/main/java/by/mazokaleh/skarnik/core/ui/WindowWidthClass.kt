package by.mazokaleh.skarnik.core.ui

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * M3 window width classes, with Flutter's breakpoints (`widgets/breakpoints.dart`): a navigation rail from
 * 600 dp, a list with the translation beside it from 840 dp.
 */
enum class WindowWidthClass {
    Compact,
    Medium,
    Expanded;

    /** A navigation rail replaces the bottom navigation bar. */
    val hasNavigationRail: Boolean get() = this != Compact

    /** Lists show the opened word beside them instead of opening it full screen. */
    val hasListDetail: Boolean get() = this == Expanded

    companion object {
        val MediumMinWidth = 600.dp
        val ExpandedMinWidth = 840.dp

        fun of(width: Dp): WindowWidthClass = when {
            width >= ExpandedMinWidth -> Expanded
            width >= MediumMinWidth -> Medium
            else -> Compact
        }
    }
}
