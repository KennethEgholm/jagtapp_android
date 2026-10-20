package dk.jagtfokus.app.jagtbart

import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.outlined.AddCircle
import androidx.compose.material.icons.outlined.Assignment
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Circle
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Place
import androidx.compose.material.icons.outlined.RemoveCircleOutline
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.stateDescription
import dk.jagtfokus.app.ui.CountCard
import dk.jagtfokus.app.ui.cardListItemColors
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dk.jagtfokus.app.R
import dk.jagtfokus.app.ui.QueryRow
import dk.jagtfokus.app.ui.SectionCard
import dk.jagtfokus.app.ui.groupTitle
import dk.jagtfokus.app.ui.theme.LocalSeasonColors
import dk.jagtfokus.app.ui.theme.Spacing
import dk.jagtfokus.core.BagEntry
import dk.jagtfokus.core.Place
import dk.jagtfokus.core.SeasonCalendar
import dk.jagtfokus.core.TextSearch
import dk.jagtfokus.core.place
import java.time.LocalDate

/** The year's bag. Empty means no entries, not "nothing taken". Port of BagListView.swift. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun YearListScreen(
    vm: JagtbartViewModel,
    bag: BagStore,
    calendar: SeasonCalendar,
    year: Int,
    snackbar: SnackbarHostState,
    onBack: () -> Unit,
    onEdit: (String) -> Unit,
    onDelete: (String) -> Unit,
) {
    val rows = bag.journal.entries(year)
    val summary = bag.journal.summary(year)
    val months = remember(rows, calendar.locale) { groupByMonth(rows, calendar) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.bag_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
            )
        },
    ) { padding ->
        LazyColumn(
            contentPadding = PaddingValues(horizontal = Spacing.m, vertical = Spacing.s),
            verticalArrangement = Arrangement.spacedBy(Spacing.s),
            modifier = Modifier.fillMaxSize().padding(padding),
        ) {
            item {
                CountCard(
                    count = summary.pieces,
                    title = pluralStringResource(R.plurals.piece_unit, summary.pieces),
                    detail = stringResource(R.string.hunting_year_of, calendar.huntingYearLabel(year)),
                )
            }
            if (rows.isEmpty()) {
                item {
                    Text(
                        stringResource(R.string.no_entries_footer),
                        Modifier.padding(Spacing.m),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            } else {
                months.forEach { month ->
                    item(key = month.key) {
                        Text(
                            month.title,
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = Spacing.xs, vertical = Spacing.xxs).semantics { heading() },
                        )
                    }
                    items(month.entries, key = { it.id }) { entry ->
                        DeletableRow(
                            onDelete = { onDelete(entry.id) },
                            actionLabel = stringResource(R.string.delete_entry, speciesName(vm, entry.speciesId)),
                        ) {
                            EntryRow(entry, vm, calendar, onEdit = onEdit)
                        }
                    }
                }
            }
        }
    }
}

private fun speciesName(vm: JagtbartViewModel, id: String): String =
    vm.catalog.allSpecies.firstOrNull { it.id == id }?.name ?: id

private data class MonthGroup(val key: String, val title: String, val entries: List<BagEntry>)

private fun groupByMonth(rows: List<BagEntry>, calendar: SeasonCalendar): List<MonthGroup> {
    val order = mutableListOf<String>()
    val grouped = mutableMapOf<String, MutableList<BagEntry>>()
    for (entry in rows) {
        val key = entry.date.take(7)
        if (grouped[key] == null) order.add(key)
        grouped.getOrPut(key) { mutableListOf() }.add(entry)
    }
    return order.map { key ->
        val list = grouped[key].orEmpty()
        val title = calendar.monthYear(list.firstOrNull()?.date ?: key).orEmpty()
            .replaceFirstChar { if (it.isLowerCase()) it.titlecase(calendar.locale) else it.toString() }
        MonthGroup(key, title, list)
    }
}

@Composable
private fun EntryRow(entry: BagEntry, vm: JagtbartViewModel, calendar: SeasonCalendar, onEdit: (String) -> Unit) {
    val name = speciesName(vm, entry.speciesId)
    val placeName = entry.placeId?.let { vm.catalog.place(it)?.name }
    val date = runCatching { LocalDate.parse(entry.date) }.getOrNull()
    val meta = listOfNotNull(
        date?.let { calendar.shortWithYear(it) } ?: entry.date,
        placeName,
    ).joinToString(" · ")
    Row(
        Modifier
            .fillMaxWidth()
            .clickable { onEdit(entry.id) }
            .padding(horizontal = Spacing.m, vertical = Spacing.s),
    ) {
        Column(Modifier.weight(1f)) {
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.s)) {
                Text(name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f, fill = false))
                Text(
                    entry.count.toString(),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text(meta, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

/**
 * Swipe left deletes, but only past half the row, so a short flick does not lose a note.
 * TalkBack has no swipe for it, so [actionLabel] is also in the actions menu.
 */
@Composable
private fun DeletableRow(onDelete: () -> Unit, actionLabel: String, content: @Composable () -> Unit) {
    val delete by rememberUpdatedState(onDelete)
    val state = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            if (value == SwipeToDismissBoxValue.EndToStart) delete()
            // Never settle on "dismissed": the saved state would bring a restored row back blank.
            false
        },
        positionalThreshold = { distance -> distance * 0.5f },
    )
    SwipeToDismissBox(
        state = state,
        modifier = Modifier.semantics(mergeDescendants = true) {
            customActions = listOf(CustomAccessibilityAction(actionLabel) { delete(); true })
        },
        enableDismissFromStartToEnd = false,
        backgroundContent = {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.errorContainer),
                contentAlignment = Alignment.CenterEnd,
            ) {
                Icon(
                    Icons.Outlined.Delete,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onErrorContainer,
                    modifier = Modifier.padding(horizontal = Spacing.m),
                )
            }
        },
    ) {
        Box(Modifier.background(MaterialTheme.colorScheme.background)) {
            content()
        }
    }
}

/** Ret one entry: date, count, place. Saves as it changes, like iOS. Port of BagEditView.swift. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BagEditScreen(vm: JagtbartViewModel, bag: BagStore, calendar: SeasonCalendar, id: String, onBack: () -> Unit, onDelete: () -> Unit) {
    val entry = bag.journal.entry(id)
    var showDate by remember { mutableStateOf(false) }
    var showPlace by remember { mutableStateOf(false) }
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.journal_edit)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
            )
        },
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Spacing.m),
        ) {
            if (entry == null) {
                Text(
                    stringResource(R.string.entry_deleted),
                    Modifier.padding(Spacing.l),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                SectionCard {
                    Column {
                        val date = runCatching { LocalDate.parse(entry.date) }.getOrNull() ?: vm.now
                        QueryRow(
                            icon = Icons.Outlined.Assignment,
                            title = stringResource(R.string.date),
                            value = calendar.fullWithYear(date).capitalized(calendar.locale),
                            detail = null,
                            hint = stringResource(R.string.choose_another_day),
                            onClick = { showDate = true },
                        )
                        CountStepper(entry.count) { newCount ->
                            bag.update(id, date, newCount, entry.placeId)
                        }
                        QueryRow(
                            icon = Icons.Outlined.Place,
                            title = stringResource(R.string.place),
                            value = entry.placeId?.let { vm.catalog.place(it)?.name } ?: stringResource(R.string.no_place),
                            detail = null,
                            hint = stringResource(R.string.choose_another_place),
                            onClick = { showPlace = true },
                        )
                    }
                }
                TextButton(
                    onClick = onDelete,
                    modifier = Modifier.padding(vertical = Spacing.s),
                ) {
                    Text(stringResource(R.string.delete), color = MaterialTheme.colorScheme.error)
                }
            }
        }
    }

    if (showDate && entry != null) {
        DatePickerSheet(
            calendar = calendar,
            today = vm.now,
            initial = runCatching { LocalDate.parse(entry.date) }.getOrDefault(vm.now),
            onDismiss = { showDate = false },
            onSelect = { picked -> bag.update(id, picked, entry.count, entry.placeId) },
        )
    }
    if (showPlace) {
        PlacePickerSheet(
            catalog = vm.catalog,
            followHere = false,
            selectedId = entry?.placeId ?: "",
            locationState = LocationState.Idle,
            gpsPlace = null,
            onDismiss = { showPlace = false },
            onHere = {},
            onSelect = { picked ->
                entry?.let { bag.update(it.id, runCatching { LocalDate.parse(it.date) }.getOrDefault(vm.now), it.count, picked.id) }
            },
            showHere = false,
            onNone = {
                entry?.let { bag.update(it.id, runCatching { LocalDate.parse(it.date) }.getOrDefault(vm.now), it.count, null) }
            },
        )
    }
}

// iOS Stepper range.
private const val MIN_COUNT = 1
private const val MAX_COUNT = 999

@Composable
private fun CountStepper(count: Int, onChange: (Int) -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .padding(horizontal = Spacing.m, vertical = Spacing.s),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            stringResource(R.string.count_label, count),
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f),
        )
        IconButton(onClick = { onChange(count - 1) }, enabled = count > MIN_COUNT) {
            Icon(Icons.Outlined.RemoveCircleOutline, contentDescription = stringResource(R.string.count_decrease))
        }
        IconButton(onClick = { onChange(count + 1) }, enabled = count < MAX_COUNT) {
            Icon(Icons.Outlined.AddCircle, contentDescription = stringResource(R.string.count_increase))
        }
    }
}

private fun String.capitalized(locale: java.util.Locale): String =
    replaceFirstChar { if (it.isLowerCase()) it.titlecase(locale) else it.toString() }

/**
 * Species taken at least once. Not the year's bag, and not a count.
 * Port of SpeciesListView.swift.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SpeciesListScreen(vm: JagtbartViewModel, bag: BagStore, onBack: () -> Unit) {
    var showAdd by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.species_list_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                },
                actions = {
                    TextButton(onClick = { showAdd = true }) { Text(stringResource(R.string.species_list_add)) }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
            )
        },
    ) { padding ->
        val known = vm.catalog.allSpecies.map { it.id }.toSet()
        val unknownIds = bag.speciesList.ids.filter { it !in known }.sorted()
        val groups = vm.catalog.groups.mapNotNull { group ->
            val species = group.species.filter { bag.speciesList.contains(it.id) }
            if (species.isEmpty()) null else group.copy(species = species)
        }
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(vertical = Spacing.s),
        ) {
            if (groups.isEmpty() && unknownIds.isEmpty()) {
                item {
                    Column(Modifier.padding(Spacing.l)) {
                        Text(stringResource(R.string.species_list_empty), color = MaterialTheme.colorScheme.onSurfaceVariant)
                        TextButton(onClick = { showAdd = true }) { Text(stringResource(R.string.species_list_add)) }
                    }
                }
            } else {
                groups.forEach { group ->
                    item(key = "group-${group.id}") {
                        Text(
                            groupTitle(group.id, group.title),
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = Spacing.l, vertical = Spacing.s).semantics { heading() },
                        )
                    }
                    items(group.species, key = { it.id }) { species ->
                        DeletableRow(
                            onDelete = { bag.removeSpecies(species.id) },
                            actionLabel = stringResource(R.string.remove_species, species.name),
                        ) {
                            Text(
                                species.name,
                                Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = Spacing.l, vertical = Spacing.s),
                            )
                        }
                    }
                }
                if (unknownIds.isNotEmpty()) {
                    item {
                        Text(
                            stringResource(R.string.unknown_group),
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = Spacing.l, vertical = Spacing.s).semantics { heading() },
                        )
                    }
                    items(unknownIds, key = { "unknown-$it" }) { id ->
                        DeletableRow(onDelete = { bag.removeSpecies(id) }, actionLabel = stringResource(R.string.remove_species, id)) {
                            Text(id, Modifier.fillMaxWidth().padding(horizontal = Spacing.l, vertical = Spacing.s))
                        }
                    }
                }
                item {
                    Text(
                        stringResource(R.string.species_list_footer),
                        Modifier.padding(Spacing.l),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }

    if (showAdd) {
        SpeciesListAddSheet(vm, bag, onDismiss = { showAdd = false })
    }
}

/** Every species, with a check. Searchable. One species, once. Port of SpeciesListAddView.swift. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SpeciesListAddSheet(vm: JagtbartViewModel, bag: BagStore, onDismiss: () -> Unit) {
    var query by rememberSaveable { mutableStateOf("") }
    val hint = stringResource(R.string.species_list_add_hint)
    val on = stringResource(R.string.selection_on)
    val off = stringResource(R.string.selection_off)
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(Modifier.fillMaxHeight(0.92f)) {
            Text(
                stringResource(R.string.species_list_add),
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(horizontal = Spacing.l, vertical = Spacing.xs),
            )
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier.fillMaxWidth().padding(horizontal = Spacing.m, vertical = Spacing.xs),
                singleLine = true,
                placeholder = { Text(stringResource(R.string.journal_search_prompt)) },
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
            val groups = vm.catalog.groups.mapNotNull { group ->
                val species = group.species.filter { TextSearch.matches(it.name, query) }
                if (species.isEmpty()) null else group.copy(species = species)
            }
            // weight(1f): a long list must not push Færdig off the sheet.
            LazyColumn(Modifier.weight(1f)) {
                if (groups.isEmpty()) {
                    item { NoResults(query) }
                }
                groups.forEach { group ->
                    item(key = "group-${group.id}") {
                        Text(
                            groupTitle(group.id, group.title),
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = Spacing.l, vertical = Spacing.s).semantics { heading() },
                        )
                    }
                    items(group.species, key = { it.id }) { item ->
                        val included = bag.speciesList.contains(item.id)
                        ListItem(
                            headlineContent = { Text(item.name) },
                            trailingContent = {
                                Icon(
                                    if (included) Icons.Outlined.CheckCircle else Icons.Outlined.Circle,
                                    contentDescription = null,
                                    tint = if (included) LocalSeasonColors.current.open else MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            },
                            colors = cardListItemColors(),
                            modifier = Modifier
                                .toggleable(value = included, role = Role.Checkbox) { bag.setSpecies(item.id, it) }
                                .semantics {
                                    stateDescription = if (included) on else off
                                    onClick(label = hint) { bag.setSpecies(item.id, !included); true }
                                },
                        )
                    }
                }
            }
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.padding(Spacing.l),
            ) { Text(stringResource(R.string.species_list_done)) }
        }
    }
}

/** Escape hatch. Keyboard only after the hunter asks for search. Port of BagSearchView.swift. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BagSearchScreen(vm: JagtbartViewModel, bag: BagStore, calendar: SeasonCalendar, onBack: () -> Unit) {
    var query by rememberSaveable { mutableStateOf("") }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.journal_search)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
            )
        },
    ) { padding ->
        val species = vm.catalog.allSpecies.filter { TextSearch.matches(it.name, query) }
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(vertical = Spacing.s),
        ) {
            item {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    modifier = Modifier.fillMaxWidth().padding(horizontal = Spacing.m, vertical = Spacing.xs),
                    singleLine = true,
                    placeholder = { Text(stringResource(R.string.journal_search_prompt)) },
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
            }
            if (species.isEmpty()) {
                item { NoResults(query) }
            }
            items(species, key = { it.id }) { item ->
                val spoken = stringResource(R.string.record_hint, item.name)
                Row(
                    Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp)
                        .clickable {
                            bag.record(
                                item.id,
                                bag.draft.date(vm.now),
                                bag.draft.placeId(vm.gpsPlace, vm.locationState),
                            )
                            onBack()
                        }
                        .padding(horizontal = Spacing.l, vertical = Spacing.s)
                        .semantics { contentDescription = spoken },
                ) {
                    Text(item.name, style = MaterialTheme.typography.headlineSmall)
                }
            }
        }
    }
}

/** iOS `ContentUnavailableView.search`: the query is named, so a typo is easy to see. */
@Composable
private fun NoResults(query: String) {
    Text(
        stringResource(R.string.no_results, query),
        Modifier.fillMaxWidth().padding(Spacing.l),
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}
