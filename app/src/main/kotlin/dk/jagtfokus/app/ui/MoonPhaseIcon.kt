package dk.jagtfokus.app.ui

import androidx.compose.foundation.Canvas
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import dk.jagtfokus.core.MoonPhase

/**
 * The eight moon phase glyphs, drawn as vector shapes. Material has no phase
 * icons, so this stands in for SF Symbols' moonphase set. Solid glyph of the
 * lit part; new moon is a ring.
 */
@Composable
fun MoonPhaseIcon(
    name: MoonPhase.Name,
    modifier: Modifier = Modifier,
    tint: Color = MaterialTheme.colorScheme.onSurfaceVariant,
) {
    Canvas(modifier) {
        val r = size.minDimension / 2f - 1.dp.toPx()
        val center = Offset(size.width / 2f, size.height / 2f)
        when (name) {
            MoonPhase.Name.Full -> drawCircle(tint, radius = r, center = center)
            MoonPhase.Name.New -> drawCircle(tint, radius = r, center = center, style = Stroke(2.dp.toPx()))
            else -> {
                // Lit fraction per phase: crescents 0.25, quarters 0.5, gibbous 0.75.
                val lit = when (name) {
                    MoonPhase.Name.FirstQuarter, MoonPhase.Name.LastQuarter -> 0.5f
                    MoonPhase.Name.WaxingGibbous, MoonPhase.Name.WaningGibbous -> 0.75f
                    else -> 0.25f
                }
                val waxing = name == MoonPhase.Name.WaxingCrescent ||
                    name == MoonPhase.Name.WaxingGibbous ||
                    name == MoonPhase.Name.FirstQuarter
                // Terminator x-radius: 0 at quarter, r at new/full.
                val k = (Math.abs(2 * lit - 1)).toFloat()
                val full = Rect(center.x - r, center.y - r, center.x + r, center.y + r)
                val small = Rect(center.x - k * r, center.y - r, center.x + k * r, center.y + r)
                val path = Path()
                // Limb: top pole to bottom pole along the lit side.
                if (waxing) path.arcTo(full, 270f, 180f, true) else path.arcTo(full, 270f, -180f, true)
                // Terminator: bottom pole back to top pole, bulging toward the limb
                // for a crescent (sliver) and away for gibbous.
                val towardLimb = lit < 0.5f
                val bulgeRight = waxing == towardLimb
                if (lit == 0.5f) {
                    path.close()
                } else if (bulgeRight) {
                    path.arcTo(small, 90f, -180f, false)
                } else {
                    path.arcTo(small, 90f, 180f, false)
                }
                path.close()
                drawPath(path, tint)
            }
        }
    }
}
