package by.mazokaleh.skarnik.feature.whatsnew.data

import android.os.Build
import by.mazokaleh.skarnik.R
import by.mazokaleh.skarnik.feature.whatsnew.domain.WhatsNewItem
import by.mazokaleh.skarnik.feature.whatsnew.domain.WhatsNewRelease

/**
 * The release notes, newest first. Add an entry in the same PR as a user-visible change, under the
 * `versionName` that will ship it; a version without an entry shows no sheet. Texts live in `strings.xml`.
 */
object WhatsNewCatalog {
    val releases: List<WhatsNewRelease> = listOf(
        WhatsNewRelease(
            version = "4.0.3",
            items = listOf(
                WhatsNewItem(
                    R.drawable.ic_accessibility_new,
                    R.string.whats_new_4_0_3_talkback_title,
                    R.string.whats_new_4_0_3_talkback_text
                ),
                WhatsNewItem(
                    R.drawable.ic_build,
                    R.string.whats_new_4_0_3_fixes_title,
                    R.string.whats_new_4_0_3_fixes_text
                )
            )
        ),
        WhatsNewRelease(
            version = "4.0.0",
            items = listOf(
                WhatsNewItem(
                    R.drawable.ic_new_releases,
                    R.string.whats_new_4_0_0_native_title,
                    R.string.whats_new_4_0_0_native_text
                ),
                WhatsNewItem(
                    R.drawable.ic_widgets,
                    R.string.whats_new_4_0_0_widget_title,
                    R.string.whats_new_4_0_0_widget_text
                ),
                WhatsNewItem(
                    R.drawable.ic_palette,
                    R.string.whats_new_4_0_0_dynamic_color_title,
                    R.string.whats_new_4_0_0_dynamic_color_text,
                    minSdk = Build.VERSION_CODES.S
                ),
                WhatsNewItem(
                    R.drawable.ic_brightness_6,
                    R.string.whats_new_4_0_0_theme_title,
                    R.string.whats_new_4_0_0_theme_text
                ),
                WhatsNewItem(
                    R.drawable.ic_tablet,
                    R.string.whats_new_4_0_0_wide_title,
                    R.string.whats_new_4_0_0_wide_text
                )
            )
        )
    )
}
