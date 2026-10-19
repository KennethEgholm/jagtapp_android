package dk.jagtfokus.app.jagtbart

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AddCircle
import androidx.compose.material.icons.outlined.Assignment
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Checklist
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.MyLocation
import androidx.compose.material.icons.outlined.Place
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import dk.jagtfokus.app.R
import dk.jagtfokus.app.ui.QueryRow
import dk.jagtfokus.app.ui.SectionCard
import dk.jagtfokus.app.ui.theme.LocalSeasonColors
import dk.jagtfokus.app.ui.theme.Spacing
import dk.jagtfokus.core.Place
import dk.jagtfokus.core.ResolvedSpecies
import dk.jagtfokus.core.SeasonCalendar
import dk.jagtfokus.core.denmark
import dk.jagtfokus.core.openGroups
import dk.jagtfokus.core.resolvedSections
import java.time.LocalDate
import java.util.Locale

/**
 * The Journal tab. One tap on a species records it. Date and place stay on
 * this tab and do not change Jagtbart. Port of BagTabView.swift.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JournalScreen(
    vm: JagtbartViewModel,
    bag: BagStore,
    calendar: SeasonCalendar,
) {
    val context = LocalContext.current
    val locale = LocalConfiguration.current.locales[0]
    var route by remember { mutableStateOf<JournalRoute>(JournalRoute.Root) }
    var showDate by remember { mutableStateOf(false) }
    var showPlace by remember { mutableStateOf(false) }
    BackHandler(route != JournalRoute.Root) { route = JournalRoute.Root }

    // The draft follows "here" until the hunter picks. Locate quietly when shown,
    // as iOS does, but without a permission dialog from this tab.
    LaunchedEffect(Unit) {
        if (bag.draft.followHere && vm.gpsPlace == null &&
            ContextCompat.checkSelfPermission(context, android.Manifest.permission.ACCESS_COARSE_LOCATION) ==
            android.content.pm.PackageManager.PERMISSION_GRANTED
        ) {
            vm.locate(locale)
        }
    }

    val bagDate = bag.draft.date(vm.now)
    val bagPlace = bag.draft.place(vm.now, vm.gpsPlace, vm.catalog.denmark)
    val openHere = remember(bagPlace.id, bagDate, vm.catalog) {
        val sections = vm.catalog.resolvedSections(bagPlace)
        calendar.openGroups(sections, bagDate).flatMap { group -> group.species }
    }
    val recent = remember(bag.journal, openHere) {
        val openIds = openHere.associateBy { species -> species.id }
        bag.journal.recentSpeciesIds().mapNotNull { openIds[it] }
    }
    val rest = remember(recent, openHere) {
        val recentIds = recent.map { species -> species.id }.toSet()
        openHere.filter { species -> species.id !in recentIds }
    }
    val year = calendar.huntingYear(vm.now)

    val currentRoute = route
    when (currentRoute) {
        JournalRoute.Root -> JournalTab(
            vm = vm,
            bag = bag,
            calendar = calendar,
            year = year,
            openHere = openHere,
            recent = recent,
            rest = rest,
            bagDate = bagDate,
            bagPlace = bagPlace,
            showDate = showDate,
            showPlace = showPlace,
            onDate = { showDate = true },
            onPlace = { showPlace = true },
            onRoute = { route = it },
            onHideDate = { showDate = false },
            onHidePlace = { showPlace = false },
        )
        JournalRoute.Year -> YearListScreen(vm, bag, calendar, year, onBack = { route = JournalRoute.Root }, onEdit = { id -> route = JournalRoute.Edit(id) })
        JournalRoute.SpeciesList -> SpeciesListScreen(vm, bag) { route = JournalRoute.Root }
        JournalRoute.Search -> BagSearchScreen(vm, bag, calendar) { route = JournalRoute.Root }
        is JournalRoute.Edit -> BagEditScreen(vm, bag, calendar, currentRoute.id) { route = JournalRoute.Root }
    }
}

private sealed interface JournalRoute {
    data object Root : JournalRoute
    data object Year : JournalRoute
    data object SpeciesList : JournalRoute
    data object Search : JournalRoute
    data class Edit(val id: String) : JournalRoute
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun JournalTab(
    vm: JagtbartViewModel,
    bag: BagStore,
    calendar: SeasonCalendar,
    year: Int,
    openHere: List<ResolvedSpecies>,
    recent: List<ResolvedSpecies>,
    rest: List<ResolvedSpecies>,
    bagDate: LocalDate,
    bagPlace: Place,
    showDate: Boolean,
    showPlace: Boolean,
    onDate: () -> Unit,
    onPlace: () -> Unit,
    onRoute: (JournalRoute) -> Unit,
    onHideDate: () -> Unit,
    onHidePlace: () -> Unit,
) {
    val context = LocalContext.current
    val season = LocalSeasonColors.current
    val summary = bag.journal.summary(year)
    val speciesListValue = if (bag.speciesList.ids.isEmpty()) {
        stringResource(R.string.no_species)
    } else {
        pluralStringResource(R.plurals.species_count, bag.speciesList.ids.size, bag.speciesList.ids.size)
    }
    val pending = bag.pending
    val pendingName = pending?.let { entry ->
        vm.catalog.allSpecies.firstOrNull { it.id == entry.speciesId }?.name
    }
    fun record(item: ResolvedSpecies) {
        bag.record(item.id, bagDate, bag.draft.placeId(vm.gpsPlace))
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            LargeTopAppBar(
                title = { Text(stringResource(R.string.journal_heading)) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
            )
        },
        bottomBar = {
            if (pending != null && pendingName != null) {
                RecordConfirmation(
                    name = pendingName,
                    count = pending.count,
                    onClose = bag::clearPending,
                    onUndo = bag::undoPending,
                    onPlusOne = bag::incrementPending,
                    onSeeYear = {
                        bag.clearPending()
                        onRoute(JournalRoute.Year)
                    },
                )
            }
        },
    ) { padding ->
        LazyColumn(
            contentPadding = PaddingValues(horizontal = Spacing.m, vertical = Spacing.xs),
            verticalArrangement = Arrangement.spacedBy(Spacing.l),
            modifier = Modifier.fillMaxSize().padding(padding),
        ) {
            item {
                SectionCard {
                    Column {
                        QueryRow(
                            icon = Icons.Outlined.Assignment,
                            title = stringResource(R.string.bag_year_row),
                            value = if (summary.pieces == 0) {
                                stringResource(R.string.no_entries)
                            } else {
                                pluralStringResource(R.plurals.pieces, summary.pieces, summary.pieces)
                            },
                            detail = stringResource(R.string.hunting_year_of, calendar.huntingYearLabel(year)),
                            hint = stringResource(R.string.bag_year_hint),
                            onClick = {
                                bag.clearPending()
                                onRoute(JournalRoute.Year)
                            },
                        )
                        QueryRow(
                            icon = Icons.Outlined.Checklist,
                            title = stringResource(R.string.species_list_title),
                            value = speciesListValue,
                            detail = null,
                            hint = stringResource(R.string.species_list_hint),
                            onClick = {
                                bag.clearPending()
                                onRoute(JournalRoute.SpeciesList)
                            },
                        )
                    }
                }
            }
            item {
                SectionCard {
                    Column {
                        QueryRow(
                            icon = Icons.Outlined.CalendarMonth,
                            title = stringResource(R.string.date),
                            value = calendar.longDate(bagDate).capitalizedFirst(calendar.locale),
                            detail = if (bagDate == vm.now) stringResource(R.string.today) else null,
                            hint = stringResource(R.string.choose_another_day),
                            onClick = onDate,
                        )
                        QueryRow(
                            icon = if (bag.draft.followHere) Icons.Outlined.MyLocation else Icons.Outlined.Place,
                            title = stringResource(R.string.place),
                            value = bagPlace.name,
                            detail = when {
                                bag.draft.followHere && vm.locationState == LocationState.Locating ->
                                    stringResource(R.string.finding_position)
                                bag.draft.followHere && vm.locationState == LocationState.Found ->
                                    stringResource(R.string.your_position)
                                else -> null
                            },
                            hint = stringResource(R.string.choose_another_place),
                            onClick = onPlace,
                        )
                    }
                }
            }
            if (recent.isNotEmpty()) {
                item { RecordSection(stringResource(R.string.recently_taken), recent, ::record) }
            }
            item {
                RecordSection(
                    if (bagDate == vm.now) stringResource(R.string.huntable_today) else stringResource(R.string.huntable_section),
                    rest,
                    ::record,
                    emptyText = if (bagDate == vm.now) stringResource(R.string.nothing_huntable_today) else stringResource(R.string.nothing_huntable),
                )
            }
            item {
                SectionCard {
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp)
                            .clickable { onRoute(JournalRoute.Search) }
                            .padding(horizontal = Spacing.m, vertical = Spacing.s),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(Spacing.s),
                    ) {
                        Icon(Icons.Outlined.Search, contentDescription = null, tint = season.forest)
                        Text(
                            stringResource(R.string.search_all_species),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                }
            }
        }
    }

    if (showDate) {
        DatePickerSheet(
            calendar = calendar,
            today = vm.now,
            initial = bagDate,
            onDismiss = onHideDate,
            onSelect = { bag.draft.select(it, vm.now) },
        )
    }
    if (showPlace) {
        PlacePickerSheet(
            catalog = vm.catalog,
            followHere = bag.draft.followHere,
            selectedId = bagPlace.id,
            locationState = vm.locationState,
            gpsPlace = vm.gpsPlace,
            onDismiss = onHidePlace,
            onHere = {
                bag.draft.useHere()
                if (ContextCompat.checkSelfPermission(context, android.Manifest.permission.ACCESS_COARSE_LOCATION) ==
                    android.content.pm.PackageManager.PERMISSION_GRANTED
                ) {
                    vm.locate(calendar.locale)
                }
            },
            onSelect = { bag.draft.select(it) },
        )
    }
}

/** A section of big tappable species rows. Tap records one piece. */
@Composable
private fun RecordSection(
    title: String,
    species: List<ResolvedSpecies>,
    onRecord: (ResolvedSpecies) -> Unit,
    emptyText: String? = null,
) {
    Column {
        Text(
            title,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = Spacing.xs, vertical = Spacing.xs),
        )
        if (species.isEmpty() && emptyText != null) {
            Text(emptyText, Modifier.padding(horizontal = Spacing.xs), color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        SectionCard {
            Column {
                species.forEach { item ->
                    RecordRow(item.name) { onRecord(item) }
                }
            }
        }
    }
}

@Composable
private fun RecordRow(name: String, onRecord: () -> Unit) {
    val spoken = stringResource(R.string.record_hint, name)
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .clickable(onClick = onRecord)
            .padding(horizontal = Spacing.m, vertical = Spacing.s)
            .semantics { contentDescription = spoken },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.s),
    ) {
        Text(
            name,
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.weight(1f),
        )
        Icon(Icons.Outlined.AddCircle, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

/** "[Art] noteret" with Fortryd, +1 and Se året. Sits at the bottom until dismissed. */
@Composable
private fun RecordConfirmation(
    name: String,
    count: Int,
    onClose: () -> Unit,
    onUndo: () -> Unit,
    onPlusOne: () -> Unit,
    onSeeYear: () -> Unit,
) {
    Surface(color = MaterialTheme.colorScheme.surfaceContainer, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(Spacing.m), verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    stringResource(R.string.journal_recorded, name),
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.weight(1f),
                )
                IconButton(onClick = onClose) {
                    Icon(
                        Icons.Outlined.Close,
                        contentDescription = stringResource(R.string.close_confirmation),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Text(
                pluralStringResource(R.plurals.pieces, count, count),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.s)) {
                TextButton(onClick = onUndo) {
                    Text(stringResource(R.string.undo), color = MaterialTheme.colorScheme.error)
                }
                TextButton(onClick = onPlusOne) {
                    Text(stringResource(R.string.plus_one), color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
                }
                TextButton(onClick = onSeeYear) { Text(stringResource(R.string.see_year)) }
            }
        }
    }
}

private fun String.capitalizedFirst(locale: Locale): String =
    replaceFirstChar { if (it.isLowerCase()) it.titlecase(locale) else it.toString() }
