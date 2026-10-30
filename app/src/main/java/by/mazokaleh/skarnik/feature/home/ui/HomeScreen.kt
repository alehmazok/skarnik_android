package by.mazokaleh.skarnik.feature.home.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import by.mazokaleh.skarnik.R
import by.mazokaleh.skarnik.ui.theme.SkarnikTheme
import kotlinx.serialization.Serializable

@Serializable
data object HomeRoute

object HomeTestTags {
    const val SEARCH = "home_search"
    const val WORD_COUNT = "home_word_count"
    const val ERROR = "home_error"
}

@Composable
fun HomeScreen(onSearchClick: () -> Unit, viewModel: HomeViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    HomeContent(uiState, onSearchClick)
}

@Composable
private fun HomeContent(uiState: HomeUiState, onSearchClick: () -> Unit) {
    Scaffold(topBar = { SearchEntry(onSearchClick) }) { padding ->
        Box(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentAlignment = Alignment.Center
        ) {
            when (uiState) {
                HomeUiState.Loading -> CircularProgressIndicator()
                is HomeUiState.Ready -> Text(
                    text = pluralStringResource(
                        R.plurals.words_in_dictionary,
                        uiState.wordCount.toInt(),
                        uiState.wordCount
                    ),
                    modifier = Modifier.testTag(HomeTestTags.WORD_COUNT),
                    style = MaterialTheme.typography.titleMedium
                )
                HomeUiState.Error -> Text(
                    text = stringResource(R.string.database_error),
                    modifier = Modifier.testTag(HomeTestTags.ERROR),
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

/**
 * A collapsed M3 search bar. It only opens the search screen, so it is a button styled as the bar rather
 * than a focusable text field (which would keep focus and reopen search on return).
 */
@Composable
private fun SearchEntry(onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.statusBars)
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .height(56.dp)
            .testTag(HomeTestTags.SEARCH),
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surfaceContainerHigh
    ) {
        Row(Modifier.padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(
                painter = painterResource(R.drawable.ic_search),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = stringResource(R.string.search_placeholder),
                modifier = Modifier.padding(start = 16.dp),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Preview
@Composable
private fun HomeContentPreview() {
    SkarnikTheme { HomeContent(HomeUiState.Ready(wordCount = 316_237), onSearchClick = {}) }
}
