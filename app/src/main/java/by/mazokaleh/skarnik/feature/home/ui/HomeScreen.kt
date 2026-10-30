package by.mazokaleh.skarnik.feature.home.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import by.mazokaleh.skarnik.R
import by.mazokaleh.skarnik.ui.theme.SkarnikTheme
import kotlinx.serialization.Serializable

@Serializable
data object HomeRoute

@Composable
fun HomeScreen(viewModel: HomeViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    HomeContent(uiState)
}

@Composable
private fun HomeContent(uiState: HomeUiState) {
    Scaffold { padding ->
        Box(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentAlignment = Alignment.Center,
        ) {
            when (uiState) {
                HomeUiState.Loading -> CircularProgressIndicator()
                is HomeUiState.Ready -> Text(
                    text = pluralStringResource(
                        R.plurals.words_in_dictionary,
                        uiState.wordCount.toInt(),
                        uiState.wordCount,
                    ),
                    style = MaterialTheme.typography.titleMedium,
                )
                HomeUiState.Error -> Text(
                    text = stringResource(R.string.database_error),
                    color = MaterialTheme.colorScheme.error,
                )
            }
        }
    }
}

@Preview
@Composable
private fun HomeContentPreview() {
    SkarnikTheme { HomeContent(HomeUiState.Ready(wordCount = 316_237)) }
}
