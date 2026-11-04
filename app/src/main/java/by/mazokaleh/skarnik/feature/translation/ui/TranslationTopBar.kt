package by.mazokaleh.skarnik.feature.translation.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import by.mazokaleh.skarnik.R
import by.mazokaleh.skarnik.feature.translation.domain.Translation

/**
 * Back, bookmark and share. With [scrollBehavior] it takes the scrolled-under colour once the content slides under
 * it (M3) and fades in the word when [showTitle] says the headline has scrolled out of view.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun TranslationTopBar(
    uiState: TranslationUiState,
    showTitle: () -> Boolean,
    scrollBehavior: TopAppBarScrollBehavior,
    onBack: (() -> Unit)?,
    onToggleBookmark: () -> Unit,
    onShare: (Translation) -> Unit
) {
    TopAppBar(
        title = {
            val word = uiState.titleWord
            AnimatedVisibility(visible = showTitle() && word != null, enter = fadeIn(), exit = fadeOut()) {
                Text(text = word.orEmpty(), maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        },
        navigationIcon = {
            if (onBack != null) {
                IconButton(onClick = onBack) {
                    Icon(
                        painter = painterResource(R.drawable.ic_arrow_back),
                        contentDescription = stringResource(R.string.action_back)
                    )
                }
            }
        },
        actions = {
            if (uiState is TranslationUiState.Loaded) {
                TranslationActions(uiState, onToggleBookmark, onShare)
            }
        },
        scrollBehavior = scrollBehavior
    )
}

private val TranslationUiState.titleWord: String?
    get() = when (this) {
        is TranslationUiState.Loading -> word
        is TranslationUiState.Error -> word
        is TranslationUiState.Loaded -> translation.title
    }

@Composable
private fun TranslationActions(
    uiState: TranslationUiState.Loaded,
    onToggleBookmark: () -> Unit,
    onShare: (Translation) -> Unit
) {
    IconButton(onClick = onToggleBookmark, modifier = Modifier.testTag(TranslationTestTags.BOOKMARK)) {
        if (uiState.bookmarked) {
            Icon(
                painter = painterResource(R.drawable.ic_bookmark),
                contentDescription = stringResource(R.string.action_bookmark_remove),
                tint = MaterialTheme.colorScheme.primary
            )
        } else {
            Icon(
                painter = painterResource(R.drawable.ic_bookmark_border),
                contentDescription = stringResource(R.string.action_bookmark_add)
            )
        }
    }
    IconButton(
        onClick = { onShare(uiState.translation) },
        modifier = Modifier.testTag(TranslationTestTags.SHARE)
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_share),
            contentDescription = stringResource(R.string.action_share)
        )
    }
}
