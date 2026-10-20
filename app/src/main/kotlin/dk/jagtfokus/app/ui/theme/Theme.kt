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

/** Udseende in Indstillinger. Stored in the "udseende" preference. */
enum class Appearance { SYSTEM, LIGHT, DARK }

/** The welcome screen's dark green brand world. Same hues as iOS. */
val BrandDeep = Color(0xFF0B2619)
val BrandGlow = Color(0xFF214C33)
val BrandGold = Color(0xFFD9B65E)
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
    val xl = 32.dp
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

/**
 * Every role is set, so no Material purple baseline shows through (nav pill, segmented
 * buttons, date picker, season bar track). Greens and grey-greens only. Brass and amber
 * stay for status and are not used as roles; `secondary` is a quiet green.
 */
private val LightScheme = lightColorScheme(
    primary = Forest,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD4E8DB),
    onPrimaryContainer = Color(0xFF0D2A1C),
    inversePrimary = ForestDark,
    secondary = Color(0xFF4F6358),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFD4E8DB),
    onSecondaryContainer = Color(0xFF0D2A1C),
    tertiary = Color(0xFF3F6470),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFD6E8EE),
    onTertiaryContainer = Color(0xFF0A2630),
    background = Color(0xFFF6F7F6),
    onBackground = Color(0xFF181C1A),
    surface = Color(0xFFF6F7F6),
    onSurface = Color(0xFF181C1A),
    surfaceVariant = Color(0xFFDDE5DF),
    onSurfaceVariant = Color(0xFF414943),
    surfaceTint = Forest,
    inverseSurface = Color(0xFF2D322F),
    inverseOnSurface = Color(0xFFEEF1EE),
    outline = Color(0xFF717A73),
    outlineVariant = Color(0xFFC1C9C3),
    scrim = Color.Black,
    surfaceBright = Color(0xFFF6F7F6),
    surfaceDim = Color(0xFFD7DBD8),
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = Color.White,
    surfaceContainer = Color(0xFFEEEFEE),
    surfaceContainerHigh = Color(0xFFE8EAE8),
    surfaceContainerHighest = Color(0xFFE1E4E2),
)

private val DarkScheme = darkColorScheme(
    primary = ForestDark,
    onPrimary = Color(0xFF0D2A1C),
    primaryContainer = Color(0xFF214D33),
    onPrimaryContainer = Color(0xFFD4E8DB),
    inversePrimary = Forest,
    secondary = Color(0xFFB5CCBD),
    onSecondary = Color(0xFF213529),
    secondaryContainer = Color(0xFF214D33),
    onSecondaryContainer = Color(0xFFD4E8DB),
    tertiary = Color(0xFFA6CBD8),
    onTertiary = Color(0xFF083543),
    tertiaryContainer = Color(0xFF254C5A),
    onTertiaryContainer = Color(0xFFD6E8EE),
    background = Color(0xFF101412),
    onBackground = Color(0xFFE0E3E0),
    surface = Color(0xFF101412),
    onSurface = Color(0xFFE0E3E0),
    surfaceVariant = Color(0xFF414943),
    onSurfaceVariant = Color(0xFFC1C9C3),
    surfaceTint = ForestDark,
    inverseSurface = Color(0xFFE0E3E0),
    inverseOnSurface = Color(0xFF2D322F),
    outline = Color(0xFF8B938D),
    outlineVariant = Color(0xFF414943),
    scrim = Color.Black,
    surfaceBright = Color(0xFF353B37),
    surfaceDim = Color(0xFF0C100E),
    surfaceContainerLowest = Color(0xFF0B0F0D),
    surfaceContainerLow = Color(0xFF1A211D),
    surfaceContainer = Color(0xFF222A25),
    surfaceContainerHigh = Color(0xFF2B332E),
    surfaceContainerHighest = Color(0xFF353D38),
)

@Composable
fun JagtFokusTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val dark = darkTheme
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
