package dk.jagtfokus.app.jagtbart

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Bedtime
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.ExpandLess
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.HourglassTop
import androidx.compose.material.icons.outlined.Air
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.MyLocation
import androidx.compose.material.icons.outlined.Park
import androidx.compose.material.icons.outlined.Pets
import androidx.compose.material.icons.outlined.Place
import androidx.compose.material.icons.outlined.Water
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import dk.jagtfokus.app.R
import dk.jagtfokus.app.location.AndroidLocationSource
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material.icons.outlined.AddCircle
import dk.jagtfokus.app.ui.IconTile
import dk.jagtfokus.app.ui.MoonPhaseIcon
import dk.jagtfokus.app.ui.QueryRow
import dk.jagtfokus.app.ui.SectionCard
import dk.jagtfokus.app.ui.capitalizedFirst
import dk.jagtfokus.app.ui.SunEventIcon
import dk.jagtfokus.app.ui.decimal
import dk.jagtfokus.app.ui.groupTitle
import dk.jagtfokus.app.ui.percent
import dk.jagtfokus.app.ui.sunHorizonIcon
import dk.jagtfokus.app.ui.weatherIcon
import dk.jagtfokus.app.ui.windFrom
import dk.jagtfokus.app.ui.theme.LocalSeasonColors
import dk.jagtfokus.app.ui.theme.Radius
import dk.jagtfokus.app.ui.theme.Sun
import dk.jagtfokus.app.ui.theme.Spacing
import kotlinx.coroutines.launch
import dk.jagtfokus.core.HuntingCatalog
import dk.jagtfokus.core.BiologyCatalog
import dk.jagtfokus.core.Place
import dk.jagtfokus.core.ResolvedGroup
import dk.jagtfokus.core.ResolvedSpecies
import dk.jagtfokus.core.SeasonCalendar
import dk.jagtfokus.core.SeasonStatus
import dk.jagtfokus.core.SpeciesPhotos
import dk.jagtfokus.core.clockTime
import dk.jagtfokus.core.closedSpecies
import dk.jagtfokus.core.coordinate
import dk.jagtfokus.core.moonPhase
import dk.jagtfokus.core.place
import dk.jagtfokus.core.sourceLink
import dk.jagtfokus.core.sunTimes
import dk.jagtfokus.core.closingSoonCount
import dk.jagtfokus.core.openGroups
import dk.jagtfokus.core.resolvedSections
import java.time.LocalDate
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JagtbartScreen(
    vm: JagtbartViewModel,
    biology: BiologyCatalog?,
    photos: SpeciesPhotos?,
    bag: BagStore,
    onSwitchToJournal: () -> Unit,
    initialSheet: String? = null,
) {
    val context = LocalContext.current
    val locale = LocalConfiguration.current.locales[0]
    val calendar = remember(locale) { SeasonCalendar(locale = locale) }
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    DisposableEffect(lifecycle) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) vm.onResume()
        }
        lifecycle.addObserver(observer)
        onDispose { lifecycle.removeObserver(observer) }
    }

    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) vm.locate(locale) else vm.onDenied()
    }
    fun hasPermission() = ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.ACCESS_COARSE_LOCATION,
    ) == PackageManager.PERMISSION_GRANTED

    LaunchedEffect(vm.followHere) {
        if (!vm.followHere) return@LaunchedEffect
        if (hasPermission()) vm.locate(locale) else permission.launch(Manifest.permission.ACCESS_COARSE_LOCATION)
    }

    var showDate by remember { mutableStateOf(false) }
    var showPlace by remember { mutableStateOf(false) }
    var showSchweiss by remember { mutableStateOf(false) }

    // Debug launch arguments, same idea as iOS: `-sheet schweiss` opens the sheet.
    LaunchedEffect(initialSheet) {
        if (initialSheet == "schweiss") showSchweiss = true
    }
    var route by remember { mutableStateOf<JagtRoute>(JagtRoute.List) }
    val folding = remember { GroupFolding(context.applicationContext) }
    val snackbar = androidx.compose.material3.SnackbarHostState()
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    val recordSwipe: (String) -> Unit = { id ->
        bag.recordAndSwitchTab(id, bag.draft.date(vm.now), bag.draft.placeId(vm.gpsPlace))
        onSwitchToJournal()
    }
    val sections = vm.catalog.resolvedSections(vm.queryPlace)
    val open = calendar.openGroups(sections, vm.queryDate)
    val closed = calendar.closedSpecies(sections, vm.queryDate)
    val openCount = open.sumOf { it.species.size }
    val closingSoon = calendar.closingSoonCount(open, vm.queryDate)
    val scroll = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val coordinate = queryCoordinate(vm)
    LaunchedEffect(coordinate, vm.queryDate) {
        val point = coordinate ?: return@LaunchedEffect
        vm.loadWeather(point.first, point.second, vm.queryDate)
    }
    androidx.activity.compose.BackHandler(route != JagtRoute.List) { route = JagtRoute.List }

    if (route == JagtRoute.Weather) {
        WeatherScreen(
            calendar = calendar,
            place = vm.queryPlace.name,
            date = vm.queryDate,
            isToday = vm.queryDate == vm.now,
            sun = coordinate?.let { calendar.sunTimes(vm.queryDate, it.first, it.second) },
            moon = calendar.moonPhase(vm.queryDate),
            weather = vm.weather,
            weatherUi = vm.weatherUi,
            onBack = { route = JagtRoute.List },
        )
        return
    }
    if (route is JagtRoute.Species) {
        val id = (route as JagtRoute.Species).id
        val item = vm.catalog.resolvedSections(vm.queryPlace).flatMap { it.species }.firstOrNull { it.id == id }
        if (item == null) {
            Text(stringResource(R.string.species_missing), Modifier.padding(Spacing.l))
        } else {
            val group = vm.catalog.groups.firstOrNull { group -> group.species.any { it.id == id } }
            SpeciesDetailScreen(
                item = item,
                groupTitle = group?.let { dk.jagtfokus.app.ui.groupTitle(it.id, it.title) },
                calendar = calendar,
                fromQuery = true,
                date = vm.queryDate,
                placeName = vm.queryPlace.name,
                localPlaceName = item.localPlaceId?.let { vm.catalog.place(it)?.name },
                status = calendar.status(item.periods, vm.queryDate),
                sourceUrl = vm.catalog.sourceLink,
                photos = photos,
                biology = biology,
                onRecordInJournal = {
                    bag.recordAndSwitchTab(item.id, bag.draft.date(vm.now), bag.draft.placeId(vm.gpsPlace))
                    onSwitchToJournal()
                },
                onBack = { route = JagtRoute.List },
            )
        }
        return
    }

    Scaffold(
        modifier = Modifier.nestedScroll(scroll.nestedScrollConnection),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            LargeTopAppBar(
                title = { Text(stringResource(R.string.tab_jagtbart)) },
                scrollBehavior = scroll,
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                ),
                actions = {
                    androidx.compose.material3.TextButton(onClick = { showSchweiss = true }) {
                        Text(stringResource(R.string.schweiss_toolbar))
                    }
                },
            )
        },
        snackbarHost = { androidx.compose.material3.SnackbarHost(snackbar) },
    ) { padding ->
        PullToRefreshBox(
            isRefreshing = vm.refreshing,
            onRefresh = {
                if (hasPermission()) vm.locate(locale) else vm.onDenied()
                vm.refreshCatalog { outcome ->
                    val text = when (outcome) {
                        dk.jagtfokus.app.data.RemoteOutcome.Updated -> context.getString(R.string.data_updated)
                        dk.jagtfokus.app.data.RemoteOutcome.BuiltInNewer -> context.getString(R.string.data_builtin_newer)
                        dk.jagtfokus.app.data.RemoteOutcome.Broken -> context.getString(R.string.data_broken)
                        dk.jagtfokus.app.data.RemoteOutcome.Offline -> context.getString(R.string.data_offline)
                        dk.jagtfokus.app.data.RemoteOutcome.HttpFailed -> context.getString(R.string.data_http)
                    }
                    scope.launch { snackbar.showSnackbar(text) }
                }
            },
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            LazyColumn(
                contentPadding = PaddingValues(horizontal = Spacing.m, vertical = Spacing.xs),
                verticalArrangement = Arrangement.spacedBy(Spacing.l),
            ) {
                item { Headline() }
                item {
                    DatePlaceCard(
                        calendar = calendar,
                        date = vm.queryDate,
                        today = vm.now,
                        place = vm.queryPlace,
                        followHere = vm.followHere,
                        locationState = vm.locationState,
                        gpsPlace = vm.gpsPlace,
                        onDate = { showDate = true },
                        onPlace = { showPlace = true },
                    )
                }
                item {
                    val point = coordinate
                    if (point != null) {
                        SunCard(
                            calendar = calendar,
                            times = calendar.sunTimes(vm.queryDate, point.first, point.second),
                            place = vm.queryPlace.name,
                            date = vm.queryDate,
                            weather = if (vm.weatherUi == WeatherUi.Loaded) vm.weather else null,
                            moon = calendar.moonPhase(vm.queryDate),
                            onClick = { route = JagtRoute.Weather },
                        )
                    }
                }
                item {
                    AnswerCard(
                        calendar = calendar,
                        count = openCount,
                        closingSoon = closingSoon,
                        date = vm.queryDate,
                        today = vm.now,
                        place = vm.queryPlace,
                        next = closed.firstOrNull(),
                        nextStatus = closed.firstOrNull()?.let { calendar.status(it.periods, vm.queryDate) },
                    )
                }
                items(open.size, key = { open[it].id }) { index ->
                    val group = open[index]
                    val soon = calendar.closingSoonCount(listOf(group), vm.queryDate)
                    GroupCard(
                        calendar = calendar,
                        group = group,
                        expanded = !folding.collapse || group.id in vm.expanded,
                        canFold = folding.collapse,
                        closingSoon = soon,
                        date = vm.queryDate,
                        onSwipeRecord = recordSwipe,

                        onToggle = { vm.toggleGroup(group.id) },
                        onSpecies = { route = JagtRoute.Species(it) },
                    )
                }
                if (closed.isNotEmpty()) {
                    item {
                        ClosedCard(
                            calendar = calendar,
                            species = closed,
                            expanded = vm.showsClosed || open.isEmpty(),
                            canFold = open.isNotEmpty(),
                            date = vm.queryDate,
                            onSwipeRecord = recordSwipe,

                            onToggle = vm::toggleClosed,
                            onSpecies = { route = JagtRoute.Species(it) },
                        )
                    }
                }
            }
        }
    }

    if (showSchweiss) {
        SchweissSheet(onDismiss = { showSchweiss = false })
    }

    if (showDate) {
        DatePickerSheet(
            calendar = calendar,
            today = vm.now,
            initial = vm.queryDate,
            onDismiss = { showDate = false },
            onSelect = {
                vm.select(it)
                showDate = false
            },
        )
    }
    if (showPlace) {
        PlacePickerSheet(
            catalog = vm.catalog,
            followHere = vm.followHere,
            selectedId = vm.pickedPlace.id,
            locationState = vm.locationState,
            gpsPlace = vm.gpsPlace,
            onDismiss = { showPlace = false },
            onHere = {
                vm.useHere()
                if (hasPermission()) vm.locate(locale) else permission.launch(Manifest.permission.ACCESS_COARSE_LOCATION)
            },
            onSelect = vm::select,
        )
    }
}

@Composable
private fun Headline() {
    Text(
        stringResource(R.string.headline),
        style = MaterialTheme.typography.headlineSmall,
        modifier = Modifier.padding(horizontal = Spacing.xs),
    )
}

@Composable
private fun DatePlaceCard(
    calendar: SeasonCalendar,
    date: LocalDate,
    today: LocalDate,
    place: Place,
    followHere: Boolean,
    locationState: LocationState,
    gpsPlace: Place?,
    onDate: () -> Unit,
    onPlace: () -> Unit,
) {
    val dateValue = calendar.longDate(date).capitalizedFirst(calendar.locale)
    val dateDetail = if (date == today) stringResource(R.string.today) else null
    val placeDetail = when {
        !followHere -> null
        locationState == LocationState.Locating -> stringResource(R.string.finding_position)
        locationState == LocationState.Found -> stringResource(R.string.your_position)
        else -> null
    }
    val problem = locationProblem(followHere, locationState, gpsPlace, place)
    SectionCard {
        Column {
            QueryRow(
                icon = Icons.Outlined.CalendarMonth,
                title = stringResource(R.string.date),
                value = dateValue,
                detail = dateDetail,
                hint = stringResource(R.string.choose_another_day),
                onClick = onDate,
            )
            QueryRow(
                icon = if (followHere) Icons.Outlined.MyLocation else Icons.Outlined.Place,
                title = stringResource(R.string.place),
                value = place.name,
                detail = placeDetail,
                hint = stringResource(R.string.choose_another_place),
                onClick = onPlace,
            )
            if (problem != null) {
                Column(Modifier.padding(horizontal = Spacing.m, vertical = Spacing.xs)) {
                    Text(
                        problem,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    if (locationState == LocationState.Denied) {
                        val context = LocalContext.current
                        TextButton(onClick = {
                            val intent = Intent(
                                Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                                Uri.fromParts("package", context.packageName, null),
                            )
                            context.startActivity(intent)
                        }) {
                            Text(stringResource(R.string.open_settings))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun locationProblem(
    followHere: Boolean,
    state: LocationState,
    gpsPlace: Place?,
    shown: Place,
): String? {
    if (!followHere) return null
    return when (state) {
        LocationState.Denied -> stringResource(R.string.location_denied)
        LocationState.Unavailable -> if (gpsPlace != null && gpsPlace.id == shown.id) {
            stringResource(R.string.location_stale, gpsPlace.name)
        } else {
            stringResource(R.string.location_unavailable)
        }
        else -> null
    }
}

@Composable
private fun AnswerCard(
    calendar: SeasonCalendar,
    count: Int,
    closingSoon: Int,
    date: LocalDate,
    today: LocalDate,
    place: Place,
    next: ResolvedSpecies?,
    nextStatus: SeasonStatus?,
) {
    val season = LocalSeasonColors.current
    val title = pluralStringResource(R.plurals.species_open, count)
    val nextOpening = nextStatus?.nextOpening
    val subtitle = when {
        count == 0 && next != null && nextOpening != null -> stringResource(
            R.string.next_opening,
            next.name,
            calendar.short(calendar.monthDay(nextOpening)),
        )
        count == 0 -> stringResource(R.string.pick_another)
        closingSoon > 0 -> pluralStringResource(R.plurals.closing_soon, closingSoon, closingSoon)
        date == today -> stringResource(R.string.today_at, place.name)
        else -> stringResource(R.string.date_at, calendar.short(calendar.monthDay(date)), place.name)
    }
    val tint = season.open.copy(alpha = if (count > 0) 0.12f else 0.05f)
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(Radius.large),
        color = tint,
    ) {
        Row(
            Modifier
                .padding(Spacing.m)
                .semantics(mergeDescendants = true) {},
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.m),
        ) {
            Text(
                count.toString(),
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Bold,
                color = if (count > 0) season.open else MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.heightIn(min = 48.dp),
            )
            Column {
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                if (closingSoon > 0 && count > 0) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.xxs)) {
                        Icon(Icons.Outlined.HourglassTop, contentDescription = null, tint = season.closingSoon, modifier = Modifier.size(16.dp))
                        Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = season.closingSoon)
                    }
                } else {
                    Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
private fun GroupCard(
    calendar: SeasonCalendar,
    group: ResolvedGroup,
    expanded: Boolean,
    canFold: Boolean,
    closingSoon: Int,
    date: LocalDate,
    onSwipeRecord: (String) -> Unit,
    onToggle: () -> Unit,
    onSpecies: (String) -> Unit,
) {
    val title = groupTitle(group.id, group.title)
    val state = if (expanded) stringResource(R.string.shown) else stringResource(R.string.hidden)
    val hint = stringResource(R.string.show_species)
    val soonText = if (closingSoon > 0) {
        pluralStringResource(R.plurals.closing_soon, closingSoon, closingSoon)
    } else {
        null
    }
    SectionCard {
        Column {
            Row(
                Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp)
                    .then(if (canFold) Modifier.clickable(onClick = onToggle) else Modifier)
                    .padding(horizontal = Spacing.m, vertical = Spacing.s)
                    .semantics {
                        contentDescription = listOfNotNull(title, group.species.size.toString(), soonText, state, hint)
                            .joinToString(". ")
                    },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Spacing.s),
            ) {
                dk.jagtfokus.app.ui.GroupSketch(group.id, Modifier.size(28.dp))
                Column(Modifier.weight(1f)) {
                    Text(title, style = MaterialTheme.typography.titleMedium)
                    if (closingSoon > 0) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.xxs)) {
                            Icon(
                                Icons.Outlined.HourglassTop,
                                contentDescription = null,
                                tint = LocalSeasonColors.current.closingSoon,
                                modifier = Modifier.size(14.dp),
                            )
                            Text(
                                soonText.orEmpty(),
                                style = MaterialTheme.typography.bodySmall,
                                color = LocalSeasonColors.current.closingSoon,
                            )
                        }
                    }
                }
                Text(
                    group.species.size.toString(),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (canFold) {
                    Icon(if (expanded) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore, contentDescription = null)
                }
            }
            if (expanded) {
                group.species.forEach { item ->
                    BagSwipe({ onSwipeRecord(item.id) }) {
                        SpeciesRow(item, calendar.status(item.periods, date), calendar) { onSpecies(item.id) }
                    }
                }
            }
        }
    }
}

@Composable
private fun ClosedCard(
    calendar: SeasonCalendar,
    species: List<ResolvedSpecies>,
    expanded: Boolean,
    canFold: Boolean,
    date: LocalDate,
    onSwipeRecord: (String) -> Unit,
    onToggle: () -> Unit,
    onSpecies: (String) -> Unit,
) {
    val title = stringResource(R.string.not_open_here)
    val shown = stringResource(R.string.shown)
    val hidden = stringResource(R.string.hidden)
    SectionCard {
        Column {
            Row(
                Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp)
                    .then(if (canFold) Modifier.clickable(onClick = onToggle) else Modifier)
                    .padding(horizontal = Spacing.m, vertical = Spacing.s)
                    .semantics {
                        contentDescription = "$title, ${species.size}" +
                            if (canFold) ". ${if (expanded) shown else hidden}" else ""
                    },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Spacing.s),
            ) {
                Icon(Icons.Outlined.Bedtime, contentDescription = null)
                Text(title, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                Text(
                    species.size.toString(),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (canFold) {
                    Icon(if (expanded) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore, contentDescription = null)
                }
            }
            if (expanded) {
                species.forEach { item ->
                    BagSwipe({ onSwipeRecord(item.id) }) {
                        SpeciesRow(item, calendar.status(item.periods, date), calendar) { onSpecies(item.id) }
                    }
                }
                Text(
                    stringResource(R.string.sorted_by_opening),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = Spacing.m, vertical = Spacing.s),
                )
            }
        }
    }
}

@Composable
private fun SpeciesRow(
    item: ResolvedSpecies,
    status: SeasonStatus,
    calendar: SeasonCalendar,
    onClick: () -> Unit,
) {
    val stacked = LocalDensity.current.fontScale >= 1.3f
    val spoken = listOfNotNull(
        item.name,
        statusLabel(status, calendar),
        calendar.spokenPeriodText(item.periods),
        if (item.isLocal) stringResource(R.string.local_season) else null,
        item.note?.takeIf { it.isNotBlank() },
    ).joinToString(". ")
    Column(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = Spacing.m, vertical = Spacing.xs)
            .semantics { contentDescription = spoken },
    ) {
        if (stacked) {
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
            calendar.periodText(item.periods),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (item.isLocal) LocalBadge()
        val note = item.note
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

private sealed interface JagtRoute {
    data object List : JagtRoute
    data object Weather : JagtRoute
    data class Species(val id: String) : JagtRoute
}
private fun queryCoordinate(vm: JagtbartViewModel): Pair<Double, Double>? {
    val latitude = vm.gpsLatitude
    val longitude = vm.gpsLongitude
    if (vm.followHere && latitude != null && longitude != null) return latitude to longitude
    return vm.catalog.coordinate(vm.queryPlace)
}

@Composable
private fun SunCard(
    calendar: SeasonCalendar,
    times: dk.jagtfokus.core.SunTimes,
    place: String,
    date: LocalDate,
    weather: dk.jagtfokus.core.MetDay?,
    moon: dk.jagtfokus.core.MoonPhase,
    onClick: () -> Unit,
) {
    val hint = stringResource(R.string.open_sun)
    SectionCard {
        Row(
            Modifier
                .fillMaxWidth()
                .heightIn(min = 48.dp)
                .clickable(onClickLabel = hint, onClick = onClick)
                .padding(Spacing.m),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(Spacing.s),
        ) {
            IconTile(sunHorizonIcon, color = Sun)
            Column(
                Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(Spacing.xxs),
            ) {
                Text(
                    stringResource(R.string.sun_weather),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                val stacked = LocalDensity.current.fontScale >= 1.3f
                if (stacked) {
                    Column(verticalArrangement = Arrangement.spacedBy(Spacing.xxs)) {
                        SunEvent(times.sunrise, rising = true, calendar)
                        SunEvent(times.sunset, rising = false, calendar)
                    }
                } else {
                    Row(horizontalArrangement = Arrangement.spacedBy(Spacing.m)) {
                        SunEvent(times.sunrise, rising = true, calendar)
                        SunEvent(times.sunset, rising = false, calendar)
                    }
                }
                if (weather != null) {
                    SunSkyLine(weather)
                    weather.maxWindSpeed?.let { speed ->
                        val direction = weather.maxWindDirection
                        val value = decimal(speed, calendar.locale)
                        val text = direction
                            ?.let { stringResource(R.string.wind_up_to_from, value, stringResource(windFrom(it))) }
                            ?: stringResource(R.string.wind_up_to, value)
                        SunLine(leading = {
                            if (direction != null) {
                                WindArrow(direction, Modifier.size(16.dp))
                            } else {
                                Icon(Icons.Outlined.Air, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(16.dp))
                            }
                        }, text = text)
                    }
                }
                SunLine(
                    leading = { MoonPhaseIcon(moon.name, Modifier.size(16.dp)) },
                    text = stringResource(R.string.lit, percent(moon.illumination, calendar.locale)),
                )
                Text(basisText(place, calendar.full(calendar.monthDay(date))), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

/** "Maks 11,9° · Min 9,1° · 0,3 mm", or what of it MET has. */
@Composable
private fun SunSkyLine(weather: dk.jagtfokus.core.MetDay) {
    val locale = LocalConfiguration.current.locales[0]
    val parts = buildList {
        val max = weather.maxTemperature
        val min = weather.minTemperature
        if (max != null && min != null) {
            add(stringResource(R.string.max_min, decimal(max, locale), decimal(min, locale)))
        }
        weather.precipitation?.let { add(decimal(it, locale, 0, 1) + " mm") }
    }
    if (parts.isNotEmpty()) {
        SunLine(
            leading = {
                Icon(
                    weatherIcon(weather.symbolCode),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(16.dp),
                )
            },
            text = parts.joinToString(" · "),
        )
    }
}

/** One secondary line with its icon in the column, so texts start in the same place. */
@Composable
private fun SunLine(leading: @Composable () -> Unit, text: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
    ) {
        Box(Modifier.size(16.dp), contentAlignment = Alignment.Center) { leading() }
        Text(text, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun SunEvent(date: java.time.Instant?, rising: Boolean, calendar: SeasonCalendar) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
    ) {
        SunEventIcon(rising, Modifier.size(16.dp))
        Text(
            date?.let(calendar::clockTime) ?: "–",
            fontWeight = FontWeight.SemiBold,
        )
    }
}
