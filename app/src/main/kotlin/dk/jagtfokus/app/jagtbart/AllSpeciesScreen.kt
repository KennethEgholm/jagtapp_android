package dk.jagtfokus.app.jagtbart

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cancel
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
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.Saver
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.ui.input.nestedscroll.nestedScroll
import dk.jagtfokus.app.ui.ReselectEffect
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import dk.jagtfokus.app.ui.appLocale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import dk.jagtfokus.app.R
import dk.jagtfokus.app.ui.GroupCard
import dk.jagtfokus.app.ui.GroupSketch
import dk.jagtfokus.app.ui.MissingSpecies
import dk.jagtfokus.app.ui.SpeciesContext
import dk.jagtfokus.app.ui.SpeciesRow
import dk.jagtfokus.app.ui.groupTitle
import dk.jagtfokus.app.ui.theme.LocalSeasonColors
import dk.jagtfokus.app.ui.theme.Spacing
import dk.jagtfokus.core.BiologyCatalog
import dk.jagtfokus.core.SeasonCalendar
import dk.jagtfokus.core.SpeciesPhotos
import dk.jagtfokus.core.TextSearch
import dk.jagtfokus.core.denmark
import dk.jagtfokus.core.resolvedSections
import dk.jagtfokus.core.sourceLink

private val IdSetSaver = Saver<Set<String>, List<String>>(save = { it.toList() }, restore = { it.toSet() })

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
    reselect: Int = 0,
) {
    val context = LocalContext.current
    val locale = appLocale()
    val calendar = remember(locale) { SeasonCalendar(locale = locale) }
    val folding = remember { GroupFolding(context.applicationContext) }
    var query by rememberSaveable { mutableStateOf("") }
    var expanded by rememberSaveable(stateSaver = IdSetSaver) { mutableStateOf(setOf<String>()) }
    var speciesId by rememberSaveable { mutableStateOf<String?>(null) }
    // Above the species switch, so Back from a page lands on the same scroll position.
    val listState = rememberLazyListState()
    val scroll = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val scope = rememberCoroutineScope()
    ReselectEffect(reselect) {
        val wasList = speciesId == null
        speciesId = null
        scroll.state.heightOffset = 0f
        scope.launch { if (wasList) listState.animateScrollToItem(0) else listState.scrollToItem(0) }
    }
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
        bag.record(id, bag.draft.date(vm.now), bag.draft.placeId(vm.gpsPlace, vm.locationState))
        onSwitchToJournal()
    }

    if (speciesId != null) {
        val item = allGroups.flatMap { it.species }.firstOrNull { it.id == speciesId }
        if (item == null) {
            MissingSpecies { speciesId = null }
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
        modifier = Modifier.fillMaxSize().nestedScroll(scroll.nestedScrollConnection),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            LargeTopAppBar(
                title = { Text(stringResource(R.string.all_species_tab)) },
                scrollBehavior = scroll,
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                ),
            )
        },
    ) { padding ->
        LazyColumn(
            state = listState,
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
                    GroupCard(
                        title = groupTitle(group.id, group.title),
                        count = group.species.size,
                        leading = { GroupSketch(group.id, Modifier.size(28.dp)) },
                        expanded = open,
                        canFold = folding.collapse && !searching,
                        foldHint = stringResource(R.string.show_species),
                        onToggle = {
                            expanded = if (group.id in expanded) expanded - group.id else expanded + group.id
                        },
                    ) {
                        Column {
                            group.species.forEach { item ->
                                BagSwipe(item.name, { recordSwipe(item.id) }) {
                                    SpeciesRow(item, SpeciesContext.General, calendar) { speciesId = item.id }
                                }
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
