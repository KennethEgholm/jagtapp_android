package dk.jagtfokus.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/** Brand tokens. Same hues as the iOS app. Not Material You: the colors mean status. */
val Forest = Color(0xFF2F6B47)
val ForestDark = Color(0xFF7FC79A)
/** Sunrise and sunset, on the sun card and the weather page. Display only, not status. */
val Sun = Color(0xFFFF9F0A)
val Brass = Color(0xFF7A5C17)
val BrassDark = Color(0xFFD9B65E)
val Amber = Color(0xFFA35200)
val AmberDark = Color(0xFFFFB547)

object Spacing {
    val xxs = 4.dp
    val xs = 8.dp
    val s = 12.dp
    val m = 16.dp
    val l = 24.dp
}

object Radius {
    val medium = 12.dp
    val large = 16.dp
    val xlarge = 22.dp
}

@Immutable
data class SeasonColors(
    val open: Color,
    val closingSoon: Color,
    val local: Color,
    /** Fixed green behind white symbols. The light accent is too pale in dark mode. */
    val forest: Color,
)

val LocalSeasonColors = staticCompositionLocalOf {
    SeasonColors(open = Forest, closingSoon = Amber, local = Brass, forest = Forest)
}

private val LightScheme = lightColorScheme(
    primary = Forest,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD4E8DB),
    onPrimaryContainer = Color(0xFF0D2A1C),
    secondary = Brass,
    background = Color(0xFFF6F7F6),
    surface = Color(0xFFF6F7F6),
    surfaceContainerLow = Color.White,
    surfaceContainer = Color(0xFFEEEFEE),
)

private val DarkScheme = darkColorScheme(
    primary = ForestDark,
    onPrimary = Color(0xFF0D2A1C),
    primaryContainer = Color(0xFF214D33),
    onPrimaryContainer = Color(0xFFD4E8DB),
    secondary = BrassDark,
    background = Color(0xFF101412),
    surface = Color(0xFF101412),
    surfaceContainerLow = Color(0xFF1A211D),
    surfaceContainer = Color(0xFF222A25),
)

@Composable
fun JagtFokusTheme(content: @Composable () -> Unit) {
    val dark = isSystemInDarkTheme()
    val season = if (dark) {
        SeasonColors(open = ForestDark, closingSoon = AmberDark, local = BrassDark, forest = Forest)
    } else {
        SeasonColors(open = Forest, closingSoon = Amber, local = Brass, forest = Forest)
    }
    CompositionLocalProvider(LocalSeasonColors provides season) {
        MaterialTheme(
            colorScheme = if (dark) DarkScheme else LightScheme,
            content = content,
        )
    }
}
