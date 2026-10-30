package by.mazokaleh.skarnik

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import by.mazokaleh.skarnik.navigation.SkarnikNavHost
import by.mazokaleh.skarnik.ui.theme.SkarnikTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            SkarnikTheme {
                SkarnikNavHost()
            }
        }
    }
}
