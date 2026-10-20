package dk.jagtfokus.app.jagtbart

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Contrast
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.Eco
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material.icons.outlined.OpenInNew
import androidx.compose.material.icons.outlined.PanTool
import androidx.compose.material.icons.outlined.Park
import androidx.compose.material.icons.outlined.Pets
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material.icons.outlined.PersonOff
import androidx.compose.material.icons.outlined.WbSunny
import androidx.compose.material.icons.outlined.WbTwilight
import androidx.compose.material.icons.outlined.WifiOff
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.foundation.lazy.rememberLazyListState
import dk.jagtfokus.app.ui.ReselectEffect
import kotlinx.coroutines.launch
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dk.jagtfokus.app.R
import dk.jagtfokus.app.ui.SectionCard
import dk.jagtfokus.app.ui.theme.Appearance
import dk.jagtfokus.app.ui.theme.Spacing
import dk.jagtfokus.core.SeasonCalendar
import dk.jagtfokus.core.sourceLink

/**
 * The settings, then Om in the same list. One tab for both.
 * Port of SettingsView.swift and AboutView.swift.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    vm: JagtbartViewModel,
    calendar: SeasonCalendar,
    appearance: Appearance,
    reselect: Int = 0,
    onAppearance: (Appearance) -> Unit,
) {
    val context = LocalContext.current
    val folding = remember { GroupFolding(context.applicationContext) }
    var showSchweiss by rememberSaveable { mutableStateOf(false) }
    BackHandler(showSchweiss) { showSchweiss = false }
    // Above the Schweiss page switch, so Back from it lands on the same scroll position.
    val listState = rememberLazyListState()
    val scroll = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val scope = rememberCoroutineScope()
    ReselectEffect(reselect) {
        val wasList = !showSchweiss
        showSchweiss = false
        scroll.state.heightOffset = 0f
        scope.launch { if (wasList) listState.animateScrollToItem(0) else listState.scrollToItem(0) }
    }

    if (showSchweiss) {
        Scaffold(
            containerColor = MaterialTheme.colorScheme.background,
            topBar = {
                TopAppBar(
                    title = { Text(stringResource(R.string.schweiss_title)) },
                    navigationIcon = {
                        IconButton(onClick = { showSchweiss = false }) {
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
                    .verticalScroll(rememberScrollState()),
            ) {
                SchweissContent()
            }
        }
        return
    }

    Scaffold(
        modifier = Modifier.nestedScroll(scroll.nestedScrollConnection),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            LargeTopAppBar(
                title = { Text(stringResource(R.string.settings_tab)) },
                scrollBehavior = scroll,
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
            )
        },
    ) { padding ->
        LazyColumn(
            state = listState,
            contentPadding = PaddingValues(horizontal = Spacing.m, vertical = Spacing.s),
            verticalArrangement = Arrangement.spacedBy(Spacing.l),
            modifier = Modifier.fillMaxSize().padding(padding),
        ) {
            item { SectionHeader(stringResource(R.string.appearance)) }
            item {
                SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                    val options = listOf(
                        Appearance.SYSTEM to Icons.Outlined.Contrast,
                        Appearance.LIGHT to Icons.Outlined.WbSunny,
                        Appearance.DARK to Icons.Outlined.DarkMode,
                    )
                    options.forEachIndexed { index, (option, icon) ->
                        SegmentedButton(
                            selected = appearance == option,
                            onClick = { onAppearance(option) },
                            shape = SegmentedButtonDefaults.itemShape(index = index, count = options.size),
                            icon = { Icon(icon, contentDescription = null, modifier = Modifier.size(16.dp)) },
                            label = { Text(stringResource(option.titleRes())) },
                        )
                    }
                }
                SectionFooter(stringResource(R.string.appearance_footer))
            }
            item { SectionHeader(stringResource(R.string.lists_header)) }
            item {
                SectionCard {
                    ListItem(
                        headlineContent = { Text(stringResource(R.string.collapse_groups)) },
                        supportingContent = { Text(stringResource(R.string.collapse_groups_footer)) },
                        trailingContent = {
                            Switch(checked = folding.collapse, onCheckedChange = folding::set)
                        },
                    )
                }
            }
            item { SectionHeader(stringResource(R.string.about_data)) }
            item {
                SectionCard {
                    Column(Modifier.padding(horizontal = Spacing.m, vertical = Spacing.s)) {
                        Text(stringResource(R.string.source), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(vm.catalog.sourceName, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
                    }
                    ListItem(
                        headlineContent = { Text(stringResource(R.string.updated)) },
                        trailingContent = { Text(updatedText(vm.catalog.updated, calendar)) },
                    )
                    val sourceLink = vm.catalog.sourceLink
                    if (sourceLink != null) {
                        val uriHandler = LocalUriHandler.current
                        ListItem(
                            headlineContent = { Text(stringResource(R.string.open_regulation), color = MaterialTheme.colorScheme.primary) },
                            leadingContent = { Icon(Icons.Outlined.OpenInNew, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                            modifier = Modifier.clickable {
                                runCatching { uriHandler.openUri(sourceLink) }
                            },
                        )
                    }
                }
            }
            item { SectionHeader(stringResource(R.string.about_responsibility)) }
            item {
                SectionCard {
                    Text(stringResource(R.string.disclaimer), Modifier.padding(Spacing.m))
                }
            }
            item {
                SectionCard {
                    Column {
                        LabelRow(Icons.Outlined.WbTwilight, stringResource(R.string.daylight))
                        LabelRow(Icons.Outlined.Map, stringResource(R.string.deer_note))
                        LabelRow(Icons.Outlined.Eco, stringResource(R.string.biology_disclaimer))
                    }
                }
                SectionFooter(stringResource(R.string.not_the_law))
            }
            item {
                SectionCard {
                    ListItem(
                        headlineContent = { Text(stringResource(R.string.schweiss_title)) },
                        leadingContent = { Icon(Icons.Outlined.Pets, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                        modifier = Modifier.clickable { showSchweiss = true },
                    )
                }
                SectionFooter(stringResource(R.string.wounded_and_hit))
            }
            item { SectionHeader(stringResource(R.string.about_photos)) }
            item {
                SectionCard {
                    Column {
                        LabelRow(Icons.Outlined.PhotoLibrary, stringResource(R.string.photos_from_commons))
                        val uriHandler = LocalUriHandler.current
                        ListItem(
                            headlineContent = { Text(stringResource(R.string.open_commons), color = MaterialTheme.colorScheme.primary) },
                            leadingContent = { Icon(Icons.Outlined.OpenInNew, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                            modifier = Modifier.clickable {
                                runCatching { uriHandler.openUri("https://commons.wikimedia.org") }
                            },
                        )
                    }
                }
                SectionFooter(stringResource(R.string.photos_licence))
            }
            item { SectionHeader(stringResource(R.string.about_privacy)) }
            item {
                SectionCard {
                    Column {
                        LabelRow(Icons.Outlined.PersonOff, stringResource(R.string.no_account))
                        LabelRow(Icons.Outlined.PanTool, stringResource(R.string.no_tracking))
                        LabelRow(Icons.Outlined.LocationOn, stringResource(R.string.location_stays))
                        LabelRow(Icons.Outlined.WifiOff, stringResource(R.string.works_offline))
                    }
                }
            }
            item { SectionHeader(stringResource(R.string.about_this_app)) }
            item {
                SectionCard {
                    Column(Modifier.padding(horizontal = Spacing.m, vertical = Spacing.s)) {
                        Text("JagtFokus", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        Text(
                            stringResource(R.string.app_tagline),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    ListItem(
                        headlineContent = { Text(stringResource(R.string.app_version)) },
                        trailingContent = { Text(versionText(context)) },
                    )
                    ListItem(
                        headlineContent = { Text(stringResource(R.string.data_version)) },
                        trailingContent = { Text(stringResource(R.string.data_version_value, vm.catalog.version)) },
                    )
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(horizontal = Spacing.xs),
    )
}

@Composable
private fun SectionFooter(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(horizontal = Spacing.m, vertical = Spacing.xs),
    )
}

@Composable
private fun LabelRow(icon: ImageVector, text: String) {
    ListItem(
        headlineContent = { Text(text, color = MaterialTheme.colorScheme.onSurfaceVariant) },
        leadingContent = { Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant) },
    )
}

/** "2026-10-11" → "11. oktober 2026", or the raw text when it does not parse. */
@Composable
private fun updatedText(dayKey: String, calendar: SeasonCalendar): String =
    runCatching { java.time.LocalDate.parse(dayKey) }
        .map { calendar.fullWithYear(it) }
        .getOrDefault(dayKey)

private fun versionText(context: android.content.Context): String {
    val info = context.packageManager.getPackageInfo(context.packageName, 0)
    val build = if (android.os.Build.VERSION.SDK_INT >= 28) info.longVersionCode else info.versionCode.toLong()
    return "${info.versionName} ($build)"
}

private fun Appearance.titleRes(): Int = when (this) {
    Appearance.SYSTEM -> R.string.appearance_system
    Appearance.LIGHT -> R.string.appearance_light
    Appearance.DARK -> R.string.appearance_dark
}
