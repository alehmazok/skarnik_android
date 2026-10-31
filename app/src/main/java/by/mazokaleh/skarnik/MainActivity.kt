package by.mazokaleh.skarnik

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import by.mazokaleh.skarnik.core.common.withAppLocale
import by.mazokaleh.skarnik.core.model.WordLink
import by.mazokaleh.skarnik.navigation.SkarnikNavHost
import by.mazokaleh.skarnik.navigation.WordLinkIntents
import by.mazokaleh.skarnik.ui.theme.SkarnikTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    /** Words to open, from the widget. Buffered until the navigation graph collects them. */
    private val wordLinks = Channel<WordLink>(Channel.BUFFERED)
    private val wordLinkFlow = wordLinks.receiveAsFlow()

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(newBase.withAppLocale())
    }

    @OptIn(ExperimentalComposeUiApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        // A recreated activity (rotation, process death) already handled its intent.
        if (savedInstanceState == null) openWordFrom(intent)
        setContent {
            SkarnikTheme {
                // Exposes Modifier.testTag values as resource ids, so Maestro flows can select by tag.
                Box(Modifier.fillMaxSize().semantics { testTagsAsResourceId = true }) {
                    SkarnikNavHost(wordLinks = wordLinkFlow)
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        openWordFrom(intent)
    }

    private fun openWordFrom(intent: Intent) {
        WordLinkIntents.wordLink(intent)?.let { wordLinks.trySend(it) }
    }
}
