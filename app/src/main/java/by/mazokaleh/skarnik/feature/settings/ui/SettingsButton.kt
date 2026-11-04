package by.mazokaleh.skarnik.feature.settings.ui

import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import by.mazokaleh.skarnik.R

object SettingsButtonTestTags {
    const val BUTTON = "settings_button"
}

/** Opens Settings. */
@Composable
fun SettingsButton(onClick: () -> Unit) {
    IconButton(onClick = onClick, modifier = Modifier.testTag(SettingsButtonTestTags.BUTTON)) {
        Icon(painterResource(R.drawable.ic_settings), contentDescription = stringResource(R.string.settings_open))
    }
}
