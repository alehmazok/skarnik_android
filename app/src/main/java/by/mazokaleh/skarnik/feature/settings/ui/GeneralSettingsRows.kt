package by.mazokaleh.skarnik.feature.settings.ui

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import by.mazokaleh.skarnik.R
import by.mazokaleh.skarnik.core.settings.ThemeMode

object GeneralSettingsTestTags {
    const val THEME = "settings_theme"
    const val THEME_VALUE = "settings_theme_value"

    /** Followed by the mode's key: `settings_theme_option_dark`. */
    const val THEME_OPTION_PREFIX = "settings_theme_option_"
    const val DYNAMIC_COLOR = "settings_dynamic_color"
    const val ANALYTICS = "settings_analytics"
    const val CLEAR_HISTORY = "settings_clear_history"
    const val WRITE_TO_DEVELOPERS = "settings_write_to_developers"
    const val RATE_APP = "settings_rate_app"
    const val ABOUT = "settings_about"
}

private const val DISABLED_ALPHA = 0.38f

@get:StringRes
val ThemeMode.labelRes: Int
    get() = when (this) {
        ThemeMode.System -> R.string.settings_theme_system
        ThemeMode.Light -> R.string.settings_theme_light
        ThemeMode.Dark -> R.string.settings_theme_dark
    }

/**
 * A plain settings row. The tag goes on the headline: a clickable row merges its children, and Maestro sees no
 * text on the merged node.
 */
@Composable
fun SettingsRow(
    @StringRes title: Int,
    @DrawableRes icon: Int,
    tag: String,
    onClick: () -> Unit,
    enabled: Boolean = true,
    supporting: (@Composable () -> Unit)? = null
) {
    ListItem(
        headlineContent = { Text(stringResource(title), Modifier.testTag(tag)) },
        supportingContent = supporting,
        leadingContent = { Icon(painterResource(icon), contentDescription = null) },
        colors = if (enabled) {
            ListItemDefaults.colors()
        } else {
            // M3 disabled content: onSurface at 38 %.
            val disabled = MaterialTheme.colorScheme.onSurface.copy(alpha = DISABLED_ALPHA)
            ListItemDefaults.colors(headlineColor = disabled, leadingIconColor = disabled)
        },
        modifier = Modifier.clickable(enabled = enabled, onClick = onClick)
    )
}

@Composable
fun ThemeRow(themeMode: ThemeMode, onClick: () -> Unit) {
    SettingsRow(R.string.settings_theme, R.drawable.ic_brightness_6, GeneralSettingsTestTags.THEME, onClick) {
        Text(stringResource(themeMode.labelRes), Modifier.testTag(GeneralSettingsTestTags.THEME_VALUE))
    }
}

/** Material You on/off. */
@Composable
fun DynamicColorRow(enabled: Boolean, onChange: (Boolean) -> Unit) {
    SwitchRow(
        title = R.string.settings_dynamic_color,
        description = R.string.settings_dynamic_color_description,
        icon = R.drawable.ic_palette,
        tag = GeneralSettingsTestTags.DYNAMIC_COLOR,
        checked = enabled,
        onChange = onChange
    )
}

/** Analytics consent, the same answer as the first-launch dialog's (parity 8.2). */
@Composable
fun AnalyticsRow(enabled: Boolean, onChange: (Boolean) -> Unit) {
    SwitchRow(
        title = R.string.settings_analytics,
        description = R.string.settings_analytics_description,
        icon = R.drawable.ic_bar_chart,
        tag = GeneralSettingsTestTags.ANALYTICS,
        checked = enabled,
        onChange = onChange
    )
}

/** The row toggles; the switch only shows the state (one TalkBack stop). The tag goes on the headline. */
@Composable
private fun SwitchRow(
    @StringRes title: Int,
    @StringRes description: Int,
    @DrawableRes icon: Int,
    tag: String,
    checked: Boolean,
    onChange: (Boolean) -> Unit
) {
    ListItem(
        headlineContent = { Text(stringResource(title), Modifier.testTag(tag)) },
        supportingContent = { Text(stringResource(description)) },
        leadingContent = { Icon(painterResource(icon), contentDescription = null) },
        trailingContent = { Switch(checked = checked, onCheckedChange = null) },
        modifier = Modifier.toggleable(value = checked, role = Role.Switch, onValueChange = onChange)
    )
}

/** M3 single-choice dialog: picking a mode applies it and closes the dialog. */
@Composable
fun ThemeDialog(current: ThemeMode, onSelect: (ThemeMode) -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.settings_theme)) },
        text = {
            Column(Modifier.selectableGroup()) {
                ThemeMode.entries.forEach { mode ->
                    ListItem(
                        headlineContent = { Text(stringResource(mode.labelRes)) },
                        leadingContent = { RadioButton(selected = mode == current, onClick = null) },
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                        modifier = Modifier
                            .selectable(selected = mode == current, role = Role.RadioButton) { onSelect(mode) }
                            .testTag(GeneralSettingsTestTags.THEME_OPTION_PREFIX + mode.key)
                    )
                }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) } }
    )
}

@Composable
fun ClearHistoryDialog(onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.settings_clear_history_title)) },
        text = { Text(stringResource(R.string.settings_clear_history_confirmation)) },
        confirmButton = { TextButton(onClick = onConfirm) { Text(stringResource(R.string.action_clear)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) } }
    )
}
