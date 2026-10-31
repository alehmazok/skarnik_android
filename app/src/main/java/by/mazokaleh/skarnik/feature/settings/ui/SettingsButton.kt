package by.mazokaleh.skarnik.feature.settings.ui

import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import by.mazokaleh.skarnik.R

object SettingsButtonTestTags {
    const val BUTTON = "settings_button"
    const val BADGE = "settings_badge"
}

/** Opens Settings. A badge points at the offline dictionaries until Settings are opened once, as in Flutter. */
@Composable
fun SettingsButton(onClick: () -> Unit, viewModel: SettingsBadgeViewModel = hiltViewModel()) {
    val showBadge by viewModel.showBadge.collectAsStateWithLifecycle()
    IconButton(onClick = onClick, modifier = Modifier.testTag(SettingsButtonTestTags.BUTTON)) {
        BadgedBox(badge = { if (showBadge) Badge(Modifier.testTag(SettingsButtonTestTags.BADGE)) }) {
            Icon(painterResource(R.drawable.ic_settings), contentDescription = stringResource(R.string.settings_open))
        }
    }
}
