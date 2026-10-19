package dk.jagtfokus.app.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AcUnit
import androidx.compose.material.icons.outlined.Cloud
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.Thunderstorm
import androidx.compose.material.icons.outlined.Umbrella
import androidx.compose.material.icons.outlined.WbCloudy
import androidx.compose.material.icons.outlined.WbSunny
import androidx.compose.material.icons.outlined.WbTwilight
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import dk.jagtfokus.app.ui.theme.Sun

/**
 * MET Norway's symbol codes mapped to Material icons, standing in for the SF
 * Symbols table in WeatherSymbols.swift. Display only.
 */
fun weatherIcon(code: String?): ImageVector {
    val night = code?.endsWith("_night") == true
    return when (code?.substringBefore('_')) {
        "clearsky" -> if (night) Icons.Outlined.DarkMode else Icons.Outlined.WbSunny
        "fair", "partlycloudy" -> Icons.Outlined.WbCloudy
        "cloudy", "fog" -> Icons.Outlined.Cloud
        else -> when {
            code?.contains("thunder") == true -> Icons.Outlined.Thunderstorm
            code?.contains("snow") == true -> Icons.Outlined.AcUnit
            code?.contains("sleet") == true -> Icons.Outlined.Umbrella
            code?.contains("rain") == true -> Icons.Outlined.Umbrella
            else -> Icons.Outlined.Cloud
        }
    }
}

/** The sun card's tile: sun on the horizon, like SF's sun.horizon.fill. */
val sunHorizonIcon: ImageVector = Icons.Outlined.WbTwilight

/**
 * Sun on the horizon with an arrow, standing in for SF's sunrise.fill and
 * sunset.fill: the arrow points up at sunrise, down at sunset.
 */
@Composable
fun SunEventIcon(rising: Boolean, modifier: Modifier = Modifier, tint: Color = Sun) {
    Canvas(modifier) {
        val w = size.width
        val h = size.height
        val horizon = h * 0.78f
        drawLine(tint, Offset(0f, horizon), Offset(w, horizon), 1.5f, StrokeCap.Round)
        drawCircle(tint, radius = h * 0.20f, center = Offset(w / 2f, h * 0.55f))
        val tipTop = h * 0.05f
        val tipBottom = h * 0.32f
        if (rising) {
            drawLine(tint, Offset(w / 2f, tipTop), Offset(w / 2f, tipBottom), 1.5f, StrokeCap.Round)
            drawLine(tint, Offset(w / 2f - 3.5f, tipTop + 5f), Offset(w / 2f, tipTop), 1.5f, StrokeCap.Round)
            drawLine(tint, Offset(w / 2f + 3.5f, tipTop + 5f), Offset(w / 2f, tipTop), 1.5f, StrokeCap.Round)
        } else {
            drawLine(tint, Offset(w / 2f, tipTop), Offset(w / 2f, tipBottom), 1.5f, StrokeCap.Round)
            drawLine(tint, Offset(w / 2f - 3.5f, tipBottom - 5f), Offset(w / 2f, tipBottom), 1.5f, StrokeCap.Round)
            drawLine(tint, Offset(w / 2f + 3.5f, tipBottom - 5f), Offset(w / 2f, tipBottom), 1.5f, StrokeCap.Round)
        }
    }
}
