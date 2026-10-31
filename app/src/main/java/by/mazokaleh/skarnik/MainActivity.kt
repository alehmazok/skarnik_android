package by.mazokaleh.skarnik

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import by.mazokaleh.skarnik.core.common.withAppLocale
import by.mazokaleh.skarnik.core.model.WordLink
import by.mazokaleh.skarnik.core.settings.Appearance
import by.mazokaleh.skarnik.core.settings.AppearanceSettings
import by.mazokaleh.skarnik.navigation.SkarnikNavHost
import by.mazokaleh.skarnik.navigation.WordLinkIntents
import by.mazokaleh.skarnik.ui.theme.SkarnikTheme
import by.mazokaleh.skarnik.ui.theme.isDark
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.receiveAsFlow

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject
    lateinit var appearanceSettings: AppearanceSettings

    /** Words to open, from the widget. Buffered until the navigation graph collects them. */
    private val wordLinks = Channel<WordLink>(Channel.BUFFERED)
    private val wordLinkFlow = wordLinks.receiveAsFlow()

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(newBase.withAppLocale())
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        // A recreated activity (rotation, process death) already handled its intent.
        if (savedInstanceState == null) openWordFrom(intent)
        setContent {
            val appearanceFlow = remember {
                appearanceSettings.appearance.catch {
                    Log.e(TAG, "Cannot read the appearance settings", it)
                    emit(Appearance())
                }
            }
            // Null until the settings are read (a few ms): the window background shows meanwhile, so a forced
            // theme never flashes the system one first.
            val appearance by appearanceFlow.collectAsStateWithLifecycle(initialValue = null)
            appearance?.let { SkarnikApp(it) }
        }
    }

    @OptIn(ExperimentalComposeUiApi::class)
    @Composable
    private fun SkarnikApp(appearance: Appearance) {
        val darkTheme = appearance.themeMode.isDark()
        // System bar icons follow the app's theme, not the system's, when the user forces light or dark.
        DisposableEffect(darkTheme) {
            enableEdgeToEdge(
                statusBarStyle = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT) { darkTheme },
                navigationBarStyle = SystemBarStyle.auto(LIGHT_SCRIM, DARK_SCRIM) { darkTheme }
            )
            onDispose {}
        }
        SkarnikTheme(themeMode = appearance.themeMode, useDynamicColor = appearance.dynamicColor) {
            // Exposes Modifier.testTag values as resource ids, so Maestro flows can select by tag.
            Box(Modifier.fillMaxSize().semantics { testTagsAsResourceId = true }) {
                SkarnikNavHost(wordLinks = wordLinkFlow)
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

    private companion object {
        const val TAG = "MainActivity"

        // The navigation bar scrims enableEdgeToEdge() uses by default (androidx.activity).
        val LIGHT_SCRIM = Color.argb(0xe6, 0xFF, 0xFF, 0xFF)
        val DARK_SCRIM = Color.argb(0x80, 0x1b, 0x1b, 0x1b)
    }
}
