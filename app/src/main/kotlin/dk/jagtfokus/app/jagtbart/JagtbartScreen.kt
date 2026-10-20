package dk.jagtfokus.app.jagtbart

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Bedtime
import androidx.compose.material.icons.outlined.HourglassTop
import androidx.compose.material.icons.outlined.Air
import androidx.compose.material.icons.outlined.Place
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.Saver
import androidx.compose.foundation.lazy.rememberLazyListState
import dk.jagtfokus.app.ui.ReselectEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import dk.jagtfokus.app.ui.appLocale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import dk.jagtfokus.app.R
import androidx.activity.compose.BackHandler
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.onClick
import dk.jagtfokus.app.data.RemoteOutcome
import dk.jagtfokus.app.ui.DatePlaceCard
import dk.jagtfokus.app.ui.GroupCard
import dk.jagtfokus.app.ui.GroupSketch
import dk.jagtfokus.app.ui.MissingSpecies
import dk.jagtfokus.app.ui.SpeciesContext
import dk.jagtfokus.app.ui.SpeciesRow
import dk.jagtfokus.app.ui.moonWords
import dk.jagtfokus.app.ui.weatherWords
import dk.jagtfokus.app.ui.IconTile
import dk.jagtfokus.app.ui.MoonPhaseIcon
import dk.jagtfokus.app.ui.SectionCard
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JagtbartScreen(
    vm: JagtbartViewModel,
    biology: BiologyCatalog?,
    photos: SpeciesPhotos?,
    bag: BagStore,
    onSwitchToJournal: () -> Unit,
    initialSheet: String? = null,
    reselect: Int = 0,
) {
    val context = LocalContext.current
    val locale = appLocale()
    val calendar = remember(locale) { SeasonCalendar(locale = locale) }

    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) vm.locate(locale) else vm.onDenied()
    }
    fun hasPermission() = ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.ACCESS_COARSE_LOCATION,
    ) == PackageManager.PERMISSION_GRANTED

    // The system dialog comes once per session. Back on the tab, a denial is shown in the card.
    LaunchedEffect(vm.followHere) {
        if (!vm.followHere) return@LaunchedEffect
        if (hasPermission()) {
            vm.locate(locale)
        } else if (vm.claimPermissionPrompt()) {
            permission.launch(Manifest.permission.ACCESS_COARSE_LOCATION)
        }
    }

    var showDate by remember { mutableStateOf(false) }
    var showPlace by remember { mutableStateOf(false) }
    var showSchweiss by remember { mutableStateOf(false) }

    // Debug launch arguments, same idea as iOS: `-sheet schweiss` opens the sheet.
    LaunchedEffect(initialSheet) {
        if (initialSheet == "schweiss") showSchweiss = true
    }
    var route by rememberSaveable(stateSaver = JagtRouteSaver) { mutableStateOf<JagtRoute>(JagtRoute.List) }
    // Above the route switch, so Back from a page lands on the same scroll position.
    val listState = rememberLazyListState()
    val folding = remember { GroupFolding(context.applicationContext) }
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    // Resolved here, in composition, so a language change reaches the snackbar too.
    val dataUpdated = stringResource(R.string.data_updated)
    val dataBuiltInNewer = stringResource(R.string.data_builtin_newer)
    val dataBroken = stringResource(R.string.data_broken)
    val dataOffline = stringResource(R.string.data_offline)
    val dataHttp = stringResource(R.string.data_http)
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
    BackHandler(route != JagtRoute.List) { route = JagtRoute.List }
    ReselectEffect(reselect) {
        val wasList = route == JagtRoute.List
        route = JagtRoute.List
        scroll.state.heightOffset = 0f
        scope.launch { if (wasList) listState.animateScrollToItem(0) else listState.scrollToItem(0) }
    }

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
            MissingSpecies { route = JagtRoute.List }
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
                    // Label colour, not green: green means status (iOS `.tint(.primary)`).
                    val open = stringResource(R.string.schweiss_open)
                    TextButton(
                        onClick = { showSchweiss = true },
                        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.onSurface),
                        modifier = Modifier.semantics { onClick(label = open, action = null) },
                    ) {
                        Text(stringResource(R.string.schweiss_toolbar))
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
        PullToRefreshBox(
            isRefreshing = vm.refreshing,
            onRefresh = {
                if (hasPermission()) vm.locate(locale) else vm.onDenied()
                vm.reloadWeather()
                vm.refreshCatalog { outcome ->
                    val text = when (outcome) {
                        RemoteOutcome.Updated -> dataUpdated
                        RemoteOutcome.BuiltInNewer -> dataBuiltInNewer
                        RemoteOutcome.Broken -> dataBroken
                        RemoteOutcome.Offline -> dataOffline
                        RemoteOutcome.HttpFailed -> dataHttp
                    }
                    scope.launch { snackbar.showSnackbar(text) }
                }
            },
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            LazyColumn(
                state = listState,
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
                            onClick = {
                                vm.reloadWeather()
                                route = JagtRoute.Weather
                            },
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
                    OpenGroupCard(
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
        modifier = Modifier
            .padding(horizontal = Spacing.xs)
            .semantics { heading() },
    )
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
private fun OpenGroupCard(
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
    GroupCard(
        title = groupTitle(group.id, group.title),
        count = group.species.size,
        leading = { GroupSketch(group.id, Modifier.size(28.dp)) },
        expanded = expanded,
        canFold = canFold,
        foldHint = stringResource(R.string.show_species),
        onToggle = onToggle,
        closingSoon = closingSoon,
    ) {
        QueryRows(calendar, group.species, date, onSwipeRecord, onSpecies)
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
    GroupCard(
        title = stringResource(R.string.not_open_here),
        count = species.size,
        leading = { Icon(Icons.Outlined.Bedtime, contentDescription = null, modifier = Modifier.size(28.dp)) },
        expanded = expanded,
        canFold = canFold,
        foldHint = stringResource(R.string.show_closed),
        onToggle = onToggle,
    ) {
        Column {
            QueryRows(calendar, species, date, onSwipeRecord, onSpecies)
            Text(
                stringResource(R.string.sorted_by_opening),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = Spacing.m, vertical = Spacing.s),
            )
        }
    }
}

@Composable
private fun QueryRows(
    calendar: SeasonCalendar,
    species: List<ResolvedSpecies>,
    date: LocalDate,
    onSwipeRecord: (String) -> Unit,
    onSpecies: (String) -> Unit,
) {
    Column {
        species.forEach { item ->
            BagSwipe(item.name, { onSwipeRecord(item.id) }) {
                SpeciesRow(item, SpeciesContext.Query(date), calendar) { onSpecies(item.id) }
            }
        }
    }
}

private val JagtRouteSaver = Saver<JagtRoute, String>(
    save = { route ->
        when (route) {
            JagtRoute.List -> "list"
            JagtRoute.Weather -> "weather"
            is JagtRoute.Species -> "species:${route.id}"
        }
    },
    restore = { saved ->
        when {
            saved == "weather" -> JagtRoute.Weather
            saved.startsWith("species:") -> JagtRoute.Species(saved.removePrefix("species:"))
            else -> JagtRoute.List
        }
    },
)

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
    val spoken = sunSpoken(calendar, times, place, date, weather, moon)
    SectionCard {
        Row(
            Modifier
                .fillMaxWidth()
                .heightIn(min = 48.dp)
                .clickable(onClickLabel = hint, onClick = onClick)
                .padding(Spacing.m)
                .clearAndSetSemantics { contentDescription = spoken },
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

/**
 * What TalkBack reads for the sun card, like iOS `SunRow.spoken`: sunrise and sunset by name,
 * the weather in words, the wind and the moon phase. The visible card is only numbers and icons.
 */
@Composable
private fun sunSpoken(
    calendar: SeasonCalendar,
    times: dk.jagtfokus.core.SunTimes,
    place: String,
    date: LocalDate,
    weather: dk.jagtfokus.core.MetDay?,
    moon: dk.jagtfokus.core.MoonPhase,
): String {
    val locale = calendar.locale
    val dash = "–"
    val parts = buildList {
        add(
            stringResource(
                R.string.sun_spoken,
                times.sunrise?.let(calendar::clockTime) ?: dash,
                times.sunset?.let(calendar::clockTime) ?: dash,
                basisText(place, calendar.full(calendar.monthDay(date))),
            ),
        )
        if (weather != null) {
            weatherWords(weather.symbolCode)?.let { add(stringResource(it) + ".") }
            val max = weather.maxTemperature
            val min = weather.minTemperature
            if (max != null && min != null) {
                add(stringResource(R.string.spoken_max_min, decimal(max, locale), decimal(min, locale)))
            }
            weather.precipitation?.let { add(stringResource(R.string.spoken_precipitation, decimal(it, locale, 0, 1))) }
            weather.maxWindSpeed?.let { speed ->
                val value = decimal(speed, locale)
                val direction = weather.maxWindDirection
                add(
                    if (direction != null) {
                        stringResource(R.string.spoken_wind_up_to_from, value, stringResource(windFrom(direction)))
                    } else {
                        stringResource(R.string.spoken_wind_up_to, value)
                    },
                )
            }
        }
        add(stringResource(R.string.spoken_moon, stringResource(moonWords(moon.name)), percent(moon.illumination, locale)))
    }
    return parts.joinToString(" ")
}

/** "Maks 11,9° · Min 9,1° · 0,3 mm", or what of it MET has. */
@Composable
private fun SunSkyLine(weather: dk.jagtfokus.core.MetDay) {
    val locale = appLocale()
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
