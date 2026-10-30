package by.mazokaleh.skarnik.feature.translation.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import by.mazokaleh.skarnik.R
import by.mazokaleh.skarnik.core.model.Dictionary
import by.mazokaleh.skarnik.core.ui.nameRes
import kotlinx.serialization.Serializable

/** Opens a word's translation. [word] is shown while the translation loads. */
@Serializable
data class TranslationRoute(val langId: Long, val wordId: Long, val word: String)

object TranslationTestTags {
    const val WORD = "translation_word"
}

/** Placeholder: the translation feature replaces the body. Navigation and arguments are final. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TranslationScreen(route: TranslationRoute, onBack: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(route.word, modifier = Modifier.testTag(TranslationTestTags.WORD)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            painter = painterResource(R.drawable.ic_arrow_back),
                            contentDescription = stringResource(R.string.action_back)
                        )
                    }
                }
            )
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
            Dictionary.byLangId(route.langId)?.let { dictionary ->
                Text(
                    text = stringResource(dictionary.nameRes),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            Text(
                text = stringResource(R.string.translation_coming_soon),
                modifier = Modifier.padding(top = 8.dp),
                style = MaterialTheme.typography.bodyLarge
            )
        }
    }
}
