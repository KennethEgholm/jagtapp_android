package dk.jagtfokus.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.HourglassTop
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dk.jagtfokus.app.R
import dk.jagtfokus.app.ui.theme.LocalSeasonColors
import dk.jagtfokus.app.ui.theme.Spacing
import dk.jagtfokus.core.ResolvedSpecies
import dk.jagtfokus.core.SeasonCalendar
import dk.jagtfokus.core.SeasonStatus
import java.time.LocalDate

/** Where a species row is shown from. Decides whether status may appear (iOS `SpeciesContext`). */
sealed interface SpeciesContext {
    /** Jagtbart: the chosen date and place. Shows status. */
    data class Query(val date: LocalDate) : SpeciesContext

    /** Alle arter: general times only. Never status. */
    data object General : SpeciesContext
}

/**
 * One species row (iOS `SpeciesRow`). Query: name, status, the local periods, a local badge.
 * General: name, the general periods and a thin season bar. Both end with the note.
 * TalkBack gets one sentence with months in full.
 */
@Composable
fun SpeciesRow(
    item: ResolvedSpecies,
    context: SpeciesContext,
    calendar: SeasonCalendar,
    onClick: () -> Unit,
) {
    val query = context as? SpeciesContext.Query
    val status = query?.let { calendar.status(item.periods, it.date) }
    val periods = if (query != null) item.periods else item.generalPeriods
    val note = (if (query != null) item.note else item.species.note)?.takeIf { it.isNotBlank() }
    val local = stringResource(R.string.local_season)
    val spoken = listOfNotNull(
        item.name,
        status?.let(calendar::spokenStatus),
        calendar.spokenPeriodText(periods),
        if (query != null && item.isLocal) local else null,
        note,
    ).joinToString(". ")
    val stacked = LocalDensity.current.fontScale >= 1.3f
    Column(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = Spacing.m, vertical = Spacing.xs)
            .clearAndSetSemantics { contentDescription = spoken },
    ) {
        if (status == null) {
            Text(item.name, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
        } else if (stacked) {
            Text(item.name, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
            StatusText(status, calendar)
        } else {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    item.name,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f),
                )
                Spacer(Modifier.size(Spacing.xs))
                StatusText(status, calendar)
            }
        }
        Text(
            calendar.periodText(periods),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (query == null) {
            SeasonBar(periods = periods, height = 5.dp, modifier = Modifier.padding(vertical = Spacing.xs))
        } else if (item.isLocal) {
            LocalBadge()
        }
        if (note != null) {
            Text(
                note,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun StatusText(status: SeasonStatus, calendar: SeasonCalendar) {
    val season = LocalSeasonColors.current
    val label = statusLabel(status, calendar)
    if (status.isOpen && status.isClosingSoon) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.xxs)) {
            Icon(Icons.Outlined.HourglassTop, contentDescription = null, tint = season.closingSoon, modifier = Modifier.size(14.dp))
            Text(label, style = MaterialTheme.typography.bodyMedium, color = season.closingSoon, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.End)
        }
    } else {
        Text(
            label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.End,
        )
    }
}

@Composable
private fun statusLabel(status: SeasonStatus, calendar: SeasonCalendar): String {
    val end = status.endDate
    val next = status.nextOpening
    return when {
        status.isOpen && end != null && status.isClosingSoon && status.daysLeft == 0 -> stringResource(R.string.last_day)
        status.isOpen && end != null && status.isClosingSoon -> stringResource(
            R.string.last_day_date,
            calendar.short(calendar.monthDay(end)),
        )
        status.isOpen && end != null -> stringResource(R.string.until_date, calendar.short(calendar.monthDay(end)))
        next != null -> stringResource(R.string.opens_date, calendar.short(calendar.monthDay(next)))
        else -> stringResource(R.string.no_season)
    }
}

@Composable
private fun LocalBadge() {
    val color = LocalSeasonColors.current.local
    Row(
        Modifier
            .padding(top = Spacing.xxs)
            .clip(RoundedCornerShape(50))
            .background(color.copy(alpha = 0.14f))
            .padding(horizontal = Spacing.xs, vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.xxs),
    ) {
        Icon(Icons.Outlined.LocationOn, contentDescription = null, tint = color, modifier = Modifier.size(12.dp))
        Text(
            stringResource(R.string.local_season),
            style = MaterialTheme.typography.labelSmall,
            color = color,
            fontWeight = FontWeight.SemiBold,
        )
    }
}
