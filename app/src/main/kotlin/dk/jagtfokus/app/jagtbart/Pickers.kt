package dk.jagtfokus.app.jagtbart

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.MyLocation
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dk.jagtfokus.app.R
import dk.jagtfokus.app.ui.theme.LocalSeasonColors
import dk.jagtfokus.app.ui.theme.Spacing
import dk.jagtfokus.core.HuntingCatalog
import dk.jagtfokus.core.Place
import dk.jagtfokus.core.PlaceKind
import dk.jagtfokus.core.SeasonCalendar
import dk.jagtfokus.core.TextSearch
import dk.jagtfokus.core.localTimeCount
import dk.jagtfokus.core.place
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.temporal.ChronoUnit

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DatePickerSheet(
    calendar: SeasonCalendar,
    today: LocalDate,
    initial: LocalDate,
    onDismiss: () -> Unit,
    onSelect: (LocalDate) -> Unit,
) {
    val state = rememberDatePickerState(initialSelectedDateMillis = initial.toPickerMillis())
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = {
                state.selectedDateMillis?.toLocalDateUtc()?.let(onSelect)
                onDismiss()
            }) { Text(stringResource(R.string.choose), fontWeight = FontWeight.SemiBold) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
        },
    ) {
        // Scrolls, so the calendar stays reachable with large text.
        Column(Modifier.verticalScroll(rememberScrollState())) {
            Text(
                stringResource(R.string.choose_day),
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier
                    .padding(start = Spacing.l, end = Spacing.l, top = Spacing.l, bottom = Spacing.xs)
                    .semantics { heading() },
            )
            QuickPicks(calendar, today) {
                // A quick pick is the answer. Close, so "Vælg" cannot put the old date back.
                onSelect(it)
                onDismiss()
            }
            DatePicker(state = state, title = null, headline = null)
        }
    }
}

@Composable
private fun QuickPicks(calendar: SeasonCalendar, today: LocalDate, onSelect: (LocalDate) -> Unit) {
    val picks = remember(today, calendar.locale) { quickPicks(today) }
    // Scrolls sideways: "Lørdag 24. okt." and "Søndag 25. okt." do not fit beside the others.
    LazyRow(
        Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = Spacing.l, vertical = Spacing.xs),
        horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
    ) {
        items(picks, key = { it.kind }) { pick ->
            val label = when (pick.kind) {
                QuickKind.Today -> stringResource(R.string.today)
                QuickKind.Tomorrow -> stringResource(R.string.tomorrow)
                QuickKind.Saturday -> stringResource(
                    R.string.weekday_date,
                    stringResource(R.string.saturday),
                    calendar.short(calendar.monthDay(pick.date)),
                )
                QuickKind.Sunday -> stringResource(
                    R.string.weekday_date,
                    stringResource(R.string.sunday),
                    calendar.short(calendar.monthDay(pick.date)),
                )
            }
            SuggestionChip(onClick = { onSelect(pick.date) }, label = { Text(label) })
        }
    }
}

private enum class QuickKind { Today, Tomorrow, Saturday, Sunday }

private data class QuickPick(val kind: QuickKind, val date: LocalDate)

/** Today, tomorrow, and the coming weekend, when it is more than a day away. */
private fun quickPicks(today: LocalDate): List<QuickPick> {
    val picks = mutableListOf(
        QuickPick(QuickKind.Today, today),
        QuickPick(QuickKind.Tomorrow, today.plusDays(1)),
    )
    for ((kind, weekday) in listOf(QuickKind.Saturday to java.time.DayOfWeek.SATURDAY, QuickKind.Sunday to java.time.DayOfWeek.SUNDAY)) {
        val next = today.plusDays(1).let { start ->
            var date = start
            while (date.dayOfWeek != weekday) date = date.plusDays(1)
            date
        }
        if (ChronoUnit.DAYS.between(today, next) > 1) picks += QuickPick(kind, next)
    }
    return picks
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlacePickerSheet(
    catalog: HuntingCatalog,
    followHere: Boolean,
    selectedId: String,
    locationState: LocationState,
    gpsPlace: Place?,
    onDismiss: () -> Unit,
    onHere: () -> Unit,
    onSelect: (Place) -> Unit,
    /** False hides "Min position / Brug GPS", for a place that is not "here" (Ret). */
    showHere: Boolean = true,
    /** Not null adds an "Intet sted" row that calls this and closes (iOS Ret). */
    onNone: (() -> Unit)? = null,
) {
    var query by remember { mutableStateOf("") }
    val sections = placeSections(catalog, query)
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(Modifier.fillMaxHeight(0.92f).imePadding()) {
            Text(
                stringResource(R.string.choose_place),
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(horizontal = Spacing.l, vertical = Spacing.xs),
            )
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Spacing.m, vertical = Spacing.xs),
                singleLine = true,
                placeholder = { Text(stringResource(R.string.search_place)) },
                leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
                trailingIcon = {
                    if (query.isNotEmpty()) {
                        IconButton(onClick = { query = "" }) {
                            Icon(
                                Icons.Filled.Cancel,
                                contentDescription = stringResource(R.string.clear_search),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                },
            )
            LazyColumn(Modifier.weight(1f)) {
                if (query.isBlank() && showHere) {
                    item {
                        HereRow(locationState, gpsPlace, followHere) {
                            onHere()
                            onDismiss()
                        }
                        Text(
                            stringResource(R.string.here_footer),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = Spacing.l, vertical = Spacing.xs),
                        )
                    }
                }
                if (query.isBlank() && onNone != null) {
                    item {
                        NoPlaceRow(selected = !followHere && selectedId.isEmpty()) {
                            onNone()
                            onDismiss()
                        }
                    }
                }
                sections.forEach { section ->
                    item {
                        Text(
                            section.title,
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = Spacing.l, vertical = Spacing.s),
                        )
                    }
                    items(section.places, key = { it.id }) { place ->
                        PlaceRow(
                            place = place,
                            parent = parentName(catalog, place),
                            localCount = catalog.localTimeCount(place.id),
                            selected = !followHere && place.id == selectedId,
                            onClick = {
                                onSelect(place)
                                onDismiss()
                            },
                        )
                    }
                }
                if (sections.isEmpty()) {
                    item {
                        Text(
                            stringResource(R.string.no_places),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(Spacing.l),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun HereRow(
    state: LocationState,
    gpsPlace: Place?,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val subtitle = when (state) {
        LocationState.Found -> gpsPlace?.name ?: stringResource(R.string.whole_country)
        LocationState.Locating -> stringResource(R.string.finding_position)
        LocationState.Denied -> stringResource(R.string.location_off)
        LocationState.Unavailable -> stringResource(R.string.location_failed)
        LocationState.Idle -> stringResource(R.string.use_gps)
    }
    ListItem(
        headlineContent = { Text(stringResource(R.string.my_position)) },
        supportingContent = { Text(subtitle) },
        leadingContent = { Icon(Icons.Outlined.MyLocation, contentDescription = null, tint = LocalSeasonColors.current.forest) },
        trailingContent = { if (selected) SelectedMark() },
        modifier = Modifier
            .heightIn(min = 48.dp)
            .clickable(onClick = onClick)
            .semantics { this.selected = selected },
    )
}

@Composable
private fun NoPlaceRow(selected: Boolean, onClick: () -> Unit) {
    ListItem(
        headlineContent = { Text(stringResource(R.string.no_place)) },
        trailingContent = { if (selected) SelectedMark() },
        modifier = Modifier
            .heightIn(min = 48.dp)
            .clickable(onClick = onClick)
            .semantics { this.selected = selected },
    )
    HorizontalDivider()
}

@Composable
private fun PlaceRow(
    place: Place,
    parent: String?,
    localCount: Int,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val subtitle = if (place.kind == PlaceKind.COUNTRY) stringResource(R.string.general_seasons) else parent
    ListItem(
        headlineContent = { Text(place.name) },
        supportingContent = subtitle?.let { { Text(it) } },
        trailingContent = {
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                if (localCount > 0) {
                    Text(
                        pluralStringResource(R.plurals.local_seasons, localCount, localCount),
                        style = MaterialTheme.typography.labelMedium,
                        color = LocalSeasonColors.current.local,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
                if (selected) SelectedMark()
            }
        },
        modifier = Modifier
            .heightIn(min = 48.dp)
            .clickable(onClick = onClick)
            .semantics { this.selected = selected },
    )
    HorizontalDivider()
}

@Composable
private fun SelectedMark() {
    Icon(
        Icons.Outlined.Check,
        contentDescription = stringResource(R.string.selected),
        tint = MaterialTheme.colorScheme.primary,
    )
}

private data class PlaceSection(val title: String, val places: List<Place>)

@Composable
private fun placeSections(catalog: HuntingCatalog, query: String): List<PlaceSection> {
    val filtered = catalog.places.filter { place ->
        TextSearch.matches(listOf(place.name) + place.matchNames, query)
    }
    val order = listOf(
        PlaceKind.COUNTRY to stringResource(R.string.whole_country),
        PlaceKind.REGION to stringResource(R.string.regions),
        PlaceKind.AREA to stringResource(R.string.areas),
        PlaceKind.ISLAND to stringResource(R.string.islands),
    )
    return order.mapNotNull { (kind, title) ->
        val items = filtered.filter { it.kind == kind }
        if (items.isEmpty()) null else PlaceSection(title, items)
    }
}

private fun parentName(catalog: HuntingCatalog, place: Place): String? {
    if (place.kind != PlaceKind.AREA && place.kind != PlaceKind.ISLAND) return null
    return place.parentId?.let { catalog.place(it)?.name }
}

private fun LocalDate.toPickerMillis(): Long =
    atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()

private fun Long.toLocalDateUtc(): LocalDate =
    Instant.ofEpochMilli(this).atZone(ZoneOffset.UTC).toLocalDate()
