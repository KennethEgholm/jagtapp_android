package dk.jagtfokus.app.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dk.jagtfokus.app.ui.theme.LocalSeasonColors
import dk.jagtfokus.core.MonthDay
import dk.jagtfokus.core.SeasonPeriod
import dk.jagtfokus.core.SeasonTimeline

/**
 * Hunting year as a bar. Decorative. The same facts are in the text.
 * [showsMonths] adds the month initials under the bar, April first.
 */
@Composable
fun SeasonBar(
    periods: List<SeasonPeriod>,
    marker: MonthDay? = null,
    height: Dp = 12.dp,
    showsMonths: Boolean = false,
    modifier: Modifier = Modifier,
) {
    val open = LocalSeasonColors.current.open
    val track = MaterialTheme.colorScheme.surfaceContainerHighest
    val gap = MaterialTheme.colorScheme.surfaceContainerLow
    val mark = MaterialTheme.colorScheme.onSurface
    val timeline = SeasonTimeline()
    // The marker reaches 4 dp past the bar on both sides, so it gets that room.
    val reach = if (marker == null) 0.dp else MarkerReach
    Column(modifier.fillMaxWidth().clearAndSetSemantics {}) {
        Canvas(Modifier.fillMaxWidth().height(height + reach * 2)) {
            val barTop = reach.toPx()
            val barHeight = height.toPx()
            val radius = CornerRadius(barHeight / 2, barHeight / 2)
            drawRoundRect(track, topLeft = Offset(0f, barTop), size = Size(size.width, barHeight), cornerRadius = radius)
            for (segment in timeline.segments(periods)) {
                val start = size.width * segment.start.toFloat()
                val width = maxOf(barHeight, size.width * (segment.endInclusive - segment.start).toFloat())
                drawRoundRect(open, topLeft = Offset(start, barTop), size = Size(width, barHeight), cornerRadius = radius)
            }
            for (month in timeline.months.drop(1)) {
                val x = size.width * timeline.positionOfMonth(month).toFloat()
                drawRect(gap, topLeft = Offset(x, barTop), size = Size(1.dp.toPx(), barHeight))
            }
            if (marker != null) {
                val x = size.width * timeline.position(marker).toFloat()
                val half = 1.5f.dp.toPx()
                drawRoundRect(
                    mark,
                    topLeft = Offset(x - half, 0f),
                    size = Size(half * 2, size.height),
                    cornerRadius = CornerRadius(2.dp.toPx(), 2.dp.toPx()),
                )
            }
        }
        if (showsMonths) {
            Row(Modifier.fillMaxWidth()) {
                for (month in timeline.months) {
                    Text(
                        timeline.initial(month),
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

private val MarkerReach = 4.dp
