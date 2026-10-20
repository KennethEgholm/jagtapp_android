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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.outlined.ExpandLess
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import dk.jagtfokus.app.ui.appLocale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dk.jagtfokus.app.R
import dk.jagtfokus.app.ui.GroupSketch
import dk.jagtfokus.app.ui.SectionCard
import dk.jagtfokus.app.ui.SeasonBar
import dk.jagtfokus.app.ui.groupTitle
import dk.jagtfokus.app.ui.theme.LocalSeasonColors
import dk.jagtfokus.app.ui.theme.Spacing
import dk.jagtfokus.core.BiologyCatalog
import dk.jagtfokus.core.ResolvedGroup
import dk.jagtfokus.core.ResolvedSpecies
import dk.jagtfokus.core.SeasonCalendar
import dk.jagtfokus.core.SpeciesPhotos
import dk.jagtfokus.core.TextSearch
import dk.jagtfokus.core.denmark
import dk.jagtfokus.core.resolvedSections
import dk.jagtfokus.core.sourceLink

/**
 * General times for every species. No date, no place, no status.
 * Port of AllSpeciesView.swift.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AllSpeciesScreen(
    vm: JagtbartViewModel,
    biology: BiologyCatalog?,
    photos: SpeciesPhotos?,
    bag: BagStore,
    onSwitchToJournal: () -> Unit,
) {
    val context = LocalContext.current
    val locale = appLocale()
    val calendar = remember(locale) { SeasonCalendar(locale = locale) }
    val folding = remember { GroupFolding(context.applicationContext) }
    var query by remember { mutableStateOf("") }
    var expanded by remember { mutableStateOf(setOf<String>()) }
    var speciesId by remember { mutableStateOf<String?>(null) }
    val searching = query.isNotBlank()
    BackHandler(speciesId != null) { speciesId = null }

    val allGroups = remember(vm.catalog) { vm.catalog.resolvedSections(vm.catalog.denmark) }
    // Search opens the groups with hits, without the extra tap.
    val groups = remember(allGroups, query) {
        if (!searching) {
            allGroups
        } else {
            allGroups.mapNotNull { group ->
                val hits = group.species.filter { TextSearch.matches(it.name, query) }
                if (hits.isEmpty()) null else group.copy(species = hits)
            }
        }
    }

    val recordSwipe: (String) -> Unit = { id ->
        bag.recordAndSwitchTab(id, bag.draft.date(vm.now), bag.draft.placeId(vm.gpsPlace))
        onSwitchToJournal()
    }

    if (speciesId != null) {
        val item = allGroups.flatMap { it.species }.firstOrNull { it.id == speciesId }
        if (item == null) {
            Text(stringResource(R.string.species_missing), Modifier.padding(Spacing.l))
        } else {
            val group = vm.catalog.groups.firstOrNull { g -> g.species.any { it.id == item.id } }
            SpeciesDetailScreen(
                item = item,
                groupTitle = group?.let { groupTitle(it.id, it.title) },
                calendar = calendar,
                fromQuery = false,
                sourceUrl = vm.catalog.sourceLink,
                photos = photos,
                biology = biology,
                onRecordInJournal = { recordSwipe(item.id) },
                onBack = { speciesId = null },
            )
        }
        return
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            LargeTopAppBar(
                title = { Text(stringResource(R.string.all_species_tab)) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                ),
            )
        },
    ) { padding ->
        LazyColumn(
            contentPadding = PaddingValues(horizontal = Spacing.m, vertical = Spacing.xs),
            verticalArrangement = Arrangement.spacedBy(Spacing.l),
            modifier = Modifier.fillMaxSize().padding(padding),
        ) {
            item {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    placeholder = { Text(stringResource(R.string.all_species_search)) },
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
            if (!searching) {
                item {
                    Row(
                        Modifier.padding(horizontal = Spacing.xs),
                        horizontalArrangement = Arrangement.spacedBy(Spacing.s),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(Icons.Outlined.Info, contentDescription = null, tint = LocalSeasonColors.current.forest)
                        Text(
                            stringResource(R.string.all_species_info),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
            groups.forEach { group ->
                val open = !folding.collapse || searching || group.id in expanded
                item(key = group.id) {
                    GroupHeaderCard(
                        group = group,
                        expanded = open,
                        canFold = folding.collapse && !searching,
                        onToggle = {
                            expanded = if (group.id in expanded) expanded - group.id else expanded + group.id
                        },
                    )
                    if (open) {
                        group.species.forEach { item ->
                            BagSwipe({ recordSwipe(item.id) }) {
                                GeneralSpeciesRow(item, calendar) { speciesId = item.id }
                            }
                        }
                    }
                }
            }
            if (groups.isEmpty()) {
                item {
                    Text(
                        stringResource(R.string.no_results, query),
                        Modifier.padding(Spacing.l),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

/** Group card header: sketch, name, count and a chevron when it can fold. */
@Composable
private fun GroupHeaderCard(
    group: ResolvedGroup,
    expanded: Boolean,
    canFold: Boolean,
    onToggle: () -> Unit,
) {
    val title = groupTitle(group.id, group.title)
    SectionCard {
        Row(
            Modifier
                .fillMaxWidth()
                .heightIn(min = 48.dp)
                .then(if (canFold) Modifier.clickable(onClick = onToggle) else Modifier)
                .padding(horizontal = Spacing.m, vertical = Spacing.s),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.s),
        ) {
            GroupSketch(group.id, Modifier.size(28.dp))
            Text(title, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
            Text(
                group.species.size.toString(),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (canFold) {
                Icon(if (expanded) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore, contentDescription = null)
            }
        }
    }
}

/** Species row for the general context: name, general periods, a thin SeasonBar, note. */
@Composable
private fun GeneralSpeciesRow(item: ResolvedSpecies, calendar: SeasonCalendar, onClick: () -> Unit) {
    val note = item.species.note
    Column(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = Spacing.m, vertical = Spacing.s),
    ) {
        Text(item.name, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
        Text(
            calendar.periodText(item.generalPeriods),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        SeasonBar(
            periods = item.generalPeriods,
            height = 5.dp,
            modifier = Modifier.padding(vertical = Spacing.xs),
        )
        if (!note.isNullOrBlank()) {
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
