package by.mazokaleh.skarnik.core.ui

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.staticCompositionLocalOf

/**
 * The app-wide snackbar, for messages that do not belong to one screen (a downloaded update). MainActivity provides
 * it and the home screen shows it above its navigation bar; a message shown elsewhere waits until home is back.
 */
val LocalAppSnackbarHostState = staticCompositionLocalOf { SnackbarHostState() }
