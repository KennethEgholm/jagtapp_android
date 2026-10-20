package dk.jagtfokus.app.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue

/**
 * Runs [onReselect] each time the shell bumps [signal], i.e. the hunter taps the tab that is
 * already open. The first value a screen sees is not a tap, so coming back to a tab or rotating
 * never fires it. iOS pops the tab's stack to its root the same way.
 */
@Composable
fun ReselectEffect(signal: Int, onReselect: () -> Unit) {
    var first by remember { mutableStateOf(true) }
    val action by rememberUpdatedState(onReselect)
    LaunchedEffect(signal) {
        if (first) first = false else action()
    }
}
