package by.mazokaleh.skarnik.feature.settings.ui

import androidx.annotation.StringRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.fromHtml
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import by.mazokaleh.skarnik.R

object AboutTestTags {
    const val SHEET = "about_sheet"
    const val VERSION = "about_version"
}

/**
 * «Пра Скарнік» (Flutter's `AboutBottomSheet`): what the three dictionaries are, links to the sister sites, the
 * developers' email and the app version.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutSheet(onWriteToDevelopers: () -> Unit, onDismiss: () -> Unit) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            Modifier
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
                .padding(bottom = 16.dp)
                .testTag(AboutTestTags.SHEET)
        ) {
            Text(
                text = stringResource(R.string.about_title),
                modifier = Modifier.padding(horizontal = 24.dp),
                style = MaterialTheme.typography.titleLarge
            )
            val linkStyles = TextLinkStyles(
                SpanStyle(color = MaterialTheme.colorScheme.primary, textDecoration = TextDecoration.Underline)
            )
            // Links open in the browser through the default LinkAnnotation handler.
            Text(
                text = AnnotatedString.fromHtml(stringResource(R.string.about_text), linkStyles),
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp),
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                text = stringResource(R.string.about_useful_links),
                modifier = Modifier.padding(start = 24.dp, end = 24.dp, top = 8.dp),
                style = MaterialTheme.typography.titleMedium
            )
            SiteLink("starnik.by", R.string.about_starnik_description)
            SiteLink("drukarnik.app", R.string.about_drukarnik_description)
            OutlinedButton(
                onClick = onWriteToDevelopers,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp).align(Alignment.CenterHorizontally)
            ) {
                Icon(painterResource(R.drawable.ic_mail), contentDescription = null)
                Text(stringResource(R.string.settings_write_to_developers), Modifier.padding(start = 8.dp))
            }
            Text(
                text = ExternalActions.appNameAndVersion(LocalContext.current),
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp).testTag(AboutTestTags.VERSION),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun SiteLink(host: String, @StringRes description: Int) {
    val uriHandler = LocalUriHandler.current
    ListItem(
        headlineContent = { Text(host, textDecoration = TextDecoration.Underline) },
        supportingContent = { Text(stringResource(description)) },
        leadingContent = { Icon(painterResource(R.drawable.ic_link), contentDescription = null) },
        colors = ListItemDefaults.colors(
            containerColor = Color.Transparent,
            headlineColor = MaterialTheme.colorScheme.primary
        ),
        // openUri throws when no browser is installed; there is nothing else to open then.
        modifier = Modifier.clickable { runCatching { uriHandler.openUri("https://$host") } }
    )
}
