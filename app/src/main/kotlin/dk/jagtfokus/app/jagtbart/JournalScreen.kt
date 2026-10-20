package dk.jagtfokus.app.jagtbart

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material.icons.outlined.Checklist
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.Saver
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.TopAppBarScrollBehavior
import dk.jagtfokus.app.ui.ReselectEffect
import kotlinx.coroutines.launch
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import dk.jagtfokus.app.ui.appLocale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import dk.jagtfokus.app.R
import dk.jagtfokus.app.ui.DatePlaceCard
import dk.jagtfokus.app.ui.QueryRow
import dk.jagtfokus.app.ui.SectionCard
import dk.jagtfokus.app.ui.capitalizedFirst
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
    reselect: Int = 0,
) {
    val context = LocalContext.current
    val locale = appLocale()
    val scope = rememberCoroutineScope()
    var route by rememberSaveable(stateSaver = JournalRouteSaver) { mutableStateOf<JournalRoute>(JournalRoute.Root) }
    var showDate by remember { mutableStateOf(false) }
    var showPlace by remember { mutableStateOf(false) }
    // Back from Ret returns to the year list it came from; the other pages return to the root.
    BackHandler(route != JournalRoute.Root) { route = route.parent() }
    val snackbar = remember { SnackbarHostState() }
    val removedText = stringResource(R.string.entry_removed)
    val undoText = stringResource(R.string.undo)
    fun deleteWithUndo(id: String) {
        val entry = bag.journal.entry(id) ?: return
        bag.remove(id)
        scope.launch {
            snackbar.currentSnackbarData?.dismiss()
            val result = snackbar.showSnackbar(removedText, undoText, duration = SnackbarDuration.Long)
            if (result == SnackbarResult.ActionPerformed) bag.restore(entry)
        }
    }
    // A note or +1 gives a tap back. Not on the first composition, only on a new save.
    val haptic = LocalHapticFeedback.current
    val savesAtStart = remember { bag.saves }
    LaunchedEffect(bag.saves) {
        if (bag.saves != savesAtStart) haptic.performHapticFeedback(HapticFeedbackType.Confirm)
    }
    // Above the route switch, so Back from a page lands on the same scroll position.
    val listState = rememberLazyListState()
    val scroll = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    ReselectEffect(reselect) {
        val wasRoot = route == JournalRoute.Root
        route = JournalRoute.Root
        scroll.state.heightOffset = 0f
        scope.launch { if (wasRoot) listState.animateScrollToItem(0) else listState.scrollToItem(0) }
    }

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
    val bagPlace = bag.draft.place(vm.gpsPlace, vm.locationState, vm.catalog.denmark)
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
            listState = listState,
            scroll = scroll,
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
        JournalRoute.Year -> YearListScreen(
            vm, bag, calendar, year, snackbar,
            onBack = { route = JournalRoute.Root },
            onEdit = { id -> route = JournalRoute.Edit(id) },
            onDelete = ::deleteWithUndo,
        )
        JournalRoute.SpeciesList -> SpeciesListScreen(vm, bag) { route = JournalRoute.Root }
        JournalRoute.Search -> BagSearchScreen(vm, bag, calendar) { route = JournalRoute.Root }
        is JournalRoute.Edit -> BagEditScreen(
            vm, bag, calendar, currentRoute.id,
            onBack = { route = JournalRoute.Year },
            onDelete = {
                deleteWithUndo(currentRoute.id)
                route = JournalRoute.Year
            },
        )
    }
}

private val JournalRouteSaver = Saver<JournalRoute, String>(
    save = { route ->
        when (route) {
            JournalRoute.Root -> "root"
            JournalRoute.Year -> "year"
            JournalRoute.SpeciesList -> "species"
            JournalRoute.Search -> "search"
            is JournalRoute.Edit -> "edit:${route.id}"
        }
    },
    restore = { saved ->
        when {
            saved == "year" -> JournalRoute.Year
            saved == "species" -> JournalRoute.SpeciesList
            saved == "search" -> JournalRoute.Search
            saved.startsWith("edit:") -> JournalRoute.Edit(saved.removePrefix("edit:"))
            else -> JournalRoute.Root
        }
    },
)

private sealed interface JournalRoute {
    fun parent(): JournalRoute = if (this is Edit) Year else Root

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
    listState: LazyListState,
    scroll: TopAppBarScrollBehavior,
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
        bag.record(item.id, bagDate, bag.draft.placeId(vm.gpsPlace, vm.locationState))
    }

    Scaffold(
        modifier = Modifier.nestedScroll(scroll.nestedScrollConnection),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            LargeTopAppBar(
                title = { Text(stringResource(R.string.journal_heading)) },
                scrollBehavior = scroll,
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
            state = listState,
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
                            detail = calendar.huntingYearLabel(year),
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
                DatePlaceCard(
                    calendar = calendar,
                    date = bagDate,
                    today = vm.now,
                    place = bagPlace,
                    followHere = bag.draft.followHere,
                    locationState = vm.locationState,
                    gpsPlace = vm.gpsPlace,
                    onDate = onDate,
                    onPlace = onPlace,
                )
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
            modifier = Modifier.padding(horizontal = Spacing.xs, vertical = Spacing.xs).semantics { heading() },
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

/**
 * "[Art] noteret" with Fortryd, +1 and Se året. Sits at the bottom until dismissed. The text is a
 * polite live region, so TalkBack says the note and each +1 without taking focus. The buttons
 * wrap onto a second line when they do not fit, as on iOS.
 */
@OptIn(ExperimentalLayoutApi::class)
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
            Row(verticalAlignment = Alignment.Top) {
                Column(
                    Modifier
                        .weight(1f)
                        .semantics(mergeDescendants = true) { liveRegion = LiveRegionMode.Polite },
                ) {
                    Text(stringResource(R.string.journal_recorded, name), style = MaterialTheme.typography.headlineSmall)
                    Text(
                        pluralStringResource(R.plurals.pieces, count, count),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                IconButton(onClick = onClose) {
                    Icon(
                        Icons.Outlined.Close,
                        contentDescription = stringResource(R.string.close_confirmation),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(Spacing.s),
                verticalArrangement = Arrangement.spacedBy(Spacing.xs),
            ) {
                OutlinedButton(onClick = onUndo) {
                    Text(stringResource(R.string.undo), color = MaterialTheme.colorScheme.error)
                }
                Button(onClick = onPlusOne) { Text(stringResource(R.string.plus_one)) }
                OutlinedButton(onClick = onSeeYear) { Text(stringResource(R.string.see_year)) }
            }
        }
    }
}
