package dk.jagtfokus.app.jagtbart

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material.icons.outlined.OpenInNew
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import dk.jagtfokus.app.R
import dk.jagtfokus.app.ui.SeasonBar
import dk.jagtfokus.app.ui.theme.LocalSeasonColors
import dk.jagtfokus.app.ui.theme.Spacing
import dk.jagtfokus.core.BiologyCatalog
import dk.jagtfokus.core.IdentifiedPhoto
import dk.jagtfokus.core.ResolvedSpecies
import dk.jagtfokus.core.SeasonCalendar
import dk.jagtfokus.core.SeasonStatus
import dk.jagtfokus.core.SpeciesPhotos
import java.time.LocalDate
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SpeciesDetailScreen(
    item: ResolvedSpecies,
    groupTitle: String?,
    calendar: SeasonCalendar,
    date: LocalDate,
    placeName: String,
    localPlaceName: String?,
    status: SeasonStatus,
    sourceUrl: String?,
    photos: SpeciesPhotos?,
    biology: BiologyCatalog?,
    onBack: () -> Unit,
) {
    val season = LocalSeasonColors.current
    Scaffold(
        topBar = {
            LargeTopAppBar(
                title = { Text(item.name) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = stringResource(R.string.close))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
            )
        },
    ) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding)) {
            if (groupTitle != null) {
                item {
                    Text(
                        groupTitle,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = Spacing.l),
                    )
                }
            }
            val photo = photos?.photo(item.id)
            if (photo != null) {
                item {
                    SpeciesPhotoCard(
                        photo = photo,
                        speciesName = item.name,
                        modifier = Modifier.padding(horizontal = Spacing.l, vertical = Spacing.xs),
                    )
                }
            }
            item {
                ListItem(
                    headlineContent = {
                        Text(
                            if (status.isOpen) stringResource(R.string.huntable) else stringResource(R.string.not_huntable),
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = if (status.isOpen) season.open else MaterialTheme.colorScheme.onSurface,
                        )
                    },
                    supportingContent = {
                        Text("${calendar.longDate(date).capitalized(calendar.locale)} · $placeName")
                    },
                    leadingContent = {
                        Icon(
                            Icons.Outlined.CheckCircle,
                            contentDescription = null,
                            tint = if (status.isOpen) season.open else MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    },
                )
                val end = status.endDate
                val next = status.nextOpening
                when {
                    status.isOpen && end != null && status.daysLeft != null -> Fact(
                        stringResource(R.string.last_day),
                        calendar.longDate(end).capitalized(calendar.locale),
                        relative(status.daysLeft),
                    )
                    next != null && status.daysUntilOpening != null -> Fact(
                        stringResource(R.string.opens),
                        calendar.longDate(next).capitalized(calendar.locale),
                        relative(status.daysUntilOpening),
                    )
                    else -> Text(
                        stringResource(R.string.no_season_year),
                        Modifier.padding(horizontal = Spacing.l),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            item {
                Text(
                    stringResource(R.string.season_in, placeName),
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.padding(horizontal = Spacing.l, vertical = Spacing.s),
                )
                SeasonBar(
                    periods = item.periods,
                    marker = calendar.monthDay(date),
                    modifier = Modifier.padding(horizontal = Spacing.l),
                )
                if (item.periods.isEmpty()) {
                    Text(stringResource(R.string.no_season_here), Modifier.padding(Spacing.l), color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else {
                    item.periods.forEach { period ->
                        ListItem(
                            headlineContent = { Text("${calendar.full(period.from)} – ${calendar.full(period.to)}") },
                            leadingContent = { Icon(Icons.Outlined.CalendarMonth, contentDescription = null, tint = season.open) },
                        )
                    }
                }
                Text(
                    stringResource(R.string.hunting_year),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = Spacing.l, vertical = Spacing.xs),
                )
            }
            if (item.isLocal) {
                item {
                    Text(stringResource(R.string.local_season), style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(horizontal = Spacing.l, vertical = Spacing.s))
                    if (localPlaceName != null) Fact(stringResource(R.string.applies_to), localPlaceName, null)
                    Fact(stringResource(R.string.general_season), calendar.periodText(item.generalPeriods), null)
                    Text(stringResource(R.string.here_footer), Modifier.padding(Spacing.l), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            val note = item.note
            if (!note.isNullOrBlank()) {
                item {
                    Text(stringResource(R.string.note), style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(horizontal = Spacing.l, vertical = Spacing.s))
                    Text(note, Modifier.padding(horizontal = Spacing.l))
                }
            }
            if (item.species.isRedOrFallowDeer) {
                item {
                    ListItem(
                        headlineContent = { Text(stringResource(R.string.deer_note), color = MaterialTheme.colorScheme.onSurfaceVariant) },
                        leadingContent = { Icon(Icons.Outlined.Map, contentDescription = null) },
                    )
                }
            }
            val taxonBiology = biology?.biology(item.id)
            if (taxonBiology != null) {
                item { BiologySection(taxonBiology) }
            }
            if (sourceUrl != null) {
                item {
                    val context = LocalContext.current
                    Text(stringResource(R.string.source), style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(horizontal = Spacing.l, vertical = Spacing.s))
                    ListItem(
                        headlineContent = { Text(stringResource(R.string.open_regulation), color = MaterialTheme.colorScheme.primary) },
                        leadingContent = { Icon(Icons.Outlined.OpenInNew, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                        modifier = Modifier.padding(bottom = Spacing.xs),
                    )
                    // The list item above is not clickable by itself. Use a button row.
                    androidx.compose.material3.TextButton(
                        onClick = { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(sourceUrl))) },
                        modifier = Modifier.padding(horizontal = Spacing.m),
                    ) { Text(stringResource(R.string.open_regulation)) }
                    Text(stringResource(R.string.not_the_law), Modifier.padding(Spacing.l), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
private fun Fact(title: String, value: String, detail: String?) {
    ListItem(
        headlineContent = { Text(title, color = MaterialTheme.colorScheme.onSurfaceVariant) },
        trailingContent = {
            androidx.compose.foundation.layout.Column(horizontalAlignment = androidx.compose.ui.Alignment.End) {
                Text(value)
                if (detail != null) Text(detail, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        },
    )
}

@Composable
private fun relative(days: Int?): String = when (days) {
    null -> ""
    0 -> stringResource(R.string.today_lower)
    1 -> stringResource(R.string.tomorrow_lower)
    else -> stringResource(R.string.in_days, days)
}

private fun String.capitalized(locale: Locale): String =
    replaceFirstChar { if (it.isLowerCase()) it.titlecase(locale) else it.toString() }
