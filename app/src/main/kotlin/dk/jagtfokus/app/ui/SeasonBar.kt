package dk.jagtfokus.app.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dk.jagtfokus.app.ui.theme.LocalSeasonColors
import dk.jagtfokus.core.MonthDay
import dk.jagtfokus.core.SeasonPeriod
import dk.jagtfokus.core.SeasonTimeline

/** Hunting year as a bar. Decorative. The same facts are in the text. */
@Composable
fun SeasonBar(
    periods: List<SeasonPeriod>,
    marker: MonthDay? = null,
    height: Dp = 12.dp,
    modifier: Modifier = Modifier,
) {
    val open = LocalSeasonColors.current.open
    val track = MaterialTheme.colorScheme.surfaceContainerHighest
    val gap = MaterialTheme.colorScheme.surfaceContainerLow
    val mark = MaterialTheme.colorScheme.onSurface
    val timeline = SeasonTimeline()
    Box(
        modifier
            .fillMaxWidth()
            .height(if (marker == null) height else height + 8.dp)
            .clearAndSetSemantics {},
    ) {
        Canvas(Modifier.fillMaxWidth().height(height).padding(top = if (marker == null) 0.dp else 4.dp)) {
            val radius = CornerRadius(size.height / 2, size.height / 2)
            drawRoundRect(track, cornerRadius = radius)
            for (segment in timeline.segments(periods)) {
                val start = size.width * segment.start.toFloat()
                val width = maxOf(size.height, size.width * (segment.endInclusive - segment.start).toFloat())
                drawRoundRect(open, topLeft = Offset(start, 0f), size = Size(width, size.height), cornerRadius = radius)
            }
            for (month in timeline.months.drop(1)) {
                val x = size.width * timeline.positionOfMonth(month).toFloat()
                drawRect(gap, topLeft = Offset(x, 0f), size = Size(1f, size.height))
            }
            if (marker != null) {
                val x = size.width * timeline.position(marker).toFloat()
                drawRoundRect(
                    mark,
                    topLeft = Offset(x - 1.5f, -4f),
                    size = Size(3f, size.height + 8f),
                    cornerRadius = CornerRadius(2f, 2f),
                )
            }
        }
    }
}
