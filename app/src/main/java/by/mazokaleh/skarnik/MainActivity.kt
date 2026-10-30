package by.mazokaleh.skarnik

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
import by.mazokaleh.skarnik.navigation.SkarnikNavHost
import by.mazokaleh.skarnik.ui.theme.SkarnikTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @OptIn(ExperimentalComposeUiApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            SkarnikTheme {
                // Exposes Modifier.testTag values as resource ids, so Maestro flows can select by tag.
                Box(Modifier.fillMaxSize().semantics { testTagsAsResourceId = true }) {
                    SkarnikNavHost()
                }
            }
        }
    }
}
