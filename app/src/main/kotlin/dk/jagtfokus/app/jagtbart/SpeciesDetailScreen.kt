package dk.jagtfokus.app.jagtbart

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.AddCircle
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Cancel
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import dk.jagtfokus.app.R
import dk.jagtfokus.app.ui.IconTile
import dk.jagtfokus.app.ui.LabeledValueRow
import dk.jagtfokus.app.ui.LinkRow
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
    fromQuery: Boolean,
    date: LocalDate? = null,
    placeName: String? = null,
    localPlaceName: String? = null,
    status: SeasonStatus? = null,
    sourceUrl: String?,
    photos: SpeciesPhotos?,
    biology: BiologyCatalog?,
    onRecordInJournal: (() -> Unit)? = null,
    onBack: () -> Unit,
) {
    val season = LocalSeasonColors.current
    // Jagtbart shows status for the chosen date and place. Alle arter shows
    // the general times only: no date, no place, no status. Like SpeciesContext.
    val periods = if (fromQuery) item.periods else item.generalPeriods
    val note = if (fromQuery) item.note else item.species.note
    val listState = rememberLazyListState()
    // The name is a heading in the content. It moves into the bar once it has scrolled away.
    val headingGone by remember { derivedStateOf { listState.firstVisibleItemIndex > 0 } }
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    AnimatedVisibility(visible = headingGone, enter = fadeIn(), exit = fadeOut()) {
                        Text(item.name, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
            )
        },
    ) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding), state = listState) {
            item {
                Column(Modifier.padding(horizontal = Spacing.l, vertical = Spacing.xs)) {
                    if (groupTitle != null) {
                        Text(
                            groupTitle,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Text(
                        item.name,
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.semantics { heading() },
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
            if (fromQuery && status != null && date != null) {
                item {
                    ListItem(
                        modifier = Modifier.semantics(mergeDescendants = true) {},
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
                                if (status.isOpen) Icons.Outlined.CheckCircle else Icons.Outlined.Cancel,
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
                            closingSoon = status.isClosingSoon,
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
            }
            if (onRecordInJournal != null) {
                item {
                    val label = stringResource(R.string.add_to_journal)
                    ListItem(
                        headlineContent = {
                            Text(label, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
                        },
                        leadingContent = { IconTile(Icons.Outlined.AddCircle) },
                        modifier = Modifier.clickable { onRecordInJournal?.invoke() },
                    )
                }
            }
            item {
                SectionHeading(
                    if (fromQuery) stringResource(R.string.season_in, placeName.orEmpty()) else stringResource(R.string.general_season),
                )
                SeasonBar(
                    periods = periods,
                    marker = if (fromQuery) date?.let { calendar.monthDay(it) } else null,
                    modifier = Modifier.padding(horizontal = Spacing.l),
                    showsMonths = true,
                )
                if (periods.isEmpty()) {
                    Text(
                        stringResource(if (fromQuery) R.string.no_season_here else R.string.no_general_season),
                        Modifier.padding(Spacing.l),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else {
                    periods.forEach { period ->
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
            if (fromQuery && item.isLocal) {
                item {
                    SectionHeading(stringResource(R.string.local_season))
                    if (localPlaceName != null) Fact(stringResource(R.string.applies_to), localPlaceName, null)
                    Fact(stringResource(R.string.general_season), calendar.periodText(item.generalPeriods), null)
                    Text(stringResource(R.string.here_footer), Modifier.padding(Spacing.l), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            if (!note.isNullOrBlank()) {
                item {
                    SectionHeading(stringResource(R.string.note))
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
                    SectionHeading(stringResource(R.string.source))
                    LinkRow(stringResource(R.string.open_regulation), sourceUrl)
                    Text(stringResource(R.string.not_the_law), Modifier.padding(Spacing.l), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
private fun SectionHeading(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.titleSmall,
        modifier = Modifier.padding(horizontal = Spacing.l, vertical = Spacing.s).semantics { heading() },
    )
}

/** The relative day in amber when the season is about to close. The words carry the meaning too. */
@Composable
private fun Fact(title: String, value: String, detail: String?, closingSoon: Boolean = false) {
    LabeledValueRow(
        label = title,
        value = value,
        note = detail,
        noteColor = if (closingSoon) LocalSeasonColors.current.closingSoon else MaterialTheme.colorScheme.onSurfaceVariant,
        labelColor = MaterialTheme.colorScheme.onSurfaceVariant,
        valueStyle = MaterialTheme.typography.bodyLarge,
    )
}

@Composable
private fun relative(days: Int?): String = when (days) {
    null -> ""
    0 -> stringResource(R.string.today_lower)
    1 -> stringResource(R.string.tomorrow_lower)
    else -> pluralStringResource(R.plurals.in_days, days, days)
}

private fun String.capitalized(locale: Locale): String =
    replaceFirstChar { if (it.isLowerCase()) it.titlecase(locale) else it.toString() }
