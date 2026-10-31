package by.mazokaleh.skarnik.feature.stress.ui

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import by.mazokaleh.skarnik.R

object StressTestTags {
    const val BACK = "stress_back"
    const val LOADING = "stress_loading"
    const val CHOICE = "stress_choice"
    const val WORD = "stress_word"
    const val PART_OF_SPEECH = "stress_part_of_speech"
    const val CELL = "stress_cell"
    const val NOT_FOUND = "stress_not_found"
    const val ERROR = "stress_error"
    const val RETRY = "stress_retry"
}

/** The part of speech for a GrammarDB / starnik.by table name; an unknown name is shown as it is (Flutter). */
@Composable
internal fun partOfSpeech(tableName: String?): String? {
    if (tableName == null) return null
    return partOfSpeechRes(tableName)?.let { stringResource(it) } ?: tableName
}

@StringRes
private fun partOfSpeechRes(tableName: String): Int? = when (tableName) {
    "Adjectives" -> R.string.stress_part_adjectives
    "Adverbs" -> R.string.stress_part_adverbs
    "Conjunctions" -> R.string.stress_part_conjunctions
    "Interjections" -> R.string.stress_part_interjections
    "Nouns" -> R.string.stress_part_nouns
    "Numerals" -> R.string.stress_part_numerals
    "Particles" -> R.string.stress_part_particles
    "Participles" -> R.string.stress_part_participles
    "Prepositions" -> R.string.stress_part_prepositions
    "Pronouns" -> R.string.stress_part_pronouns
    "Verbs" -> R.string.stress_part_verbs
    else -> null
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun StressTopBar(onBack: () -> Unit, scrollBehavior: TopAppBarScrollBehavior? = null) {
    TopAppBar(
        title = { Text(stringResource(R.string.stress_title)) },
        navigationIcon = {
            IconButton(onClick = onBack, modifier = Modifier.testTag(StressTestTags.BACK)) {
                Icon(painterResource(R.drawable.ic_arrow_back), stringResource(R.string.action_back))
            }
        },
        scrollBehavior = scrollBehavior
    )
}

@Composable
internal fun StressLoading(modifier: Modifier = Modifier) {
    LinearProgressIndicator(modifier.fillMaxWidth().testTag(StressTestTags.LOADING))
}

/**
 * Not found, or an error with «Паўтарыць». M3: shown in the screen; Flutter closed the screen with a snackbar
 * (not found) or showed the error without a way to retry.
 */
@Composable
internal fun StressMessage(notFound: Boolean, onRetry: () -> Unit, modifier: Modifier = Modifier) {
    Column(modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(
            text = stringResource(if (notFound) R.string.stress_not_found else R.string.stress_error),
            modifier = Modifier.testTag(if (notFound) StressTestTags.NOT_FOUND else StressTestTags.ERROR),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        if (!notFound) {
            FilledTonalButton(onClick = onRetry, modifier = Modifier.testTag(StressTestTags.RETRY)) {
                Text(stringResource(R.string.action_retry))
            }
        }
    }
}
