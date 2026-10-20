package dk.jagtfokus.app

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.graphics.Color as AndroidColor
import android.os.Bundle
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.LocalActivity
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.FormatListBulleted
import androidx.compose.material.icons.outlined.MenuBook
import androidx.compose.material.icons.outlined.MyLocation
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.rememberUpdatedState
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import androidx.core.graphics.drawable.toDrawable
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.LifecycleStartEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewmodel.compose.viewModel
import dk.jagtfokus.app.data.CatalogRepository
import dk.jagtfokus.app.data.WeatherClient
import dk.jagtfokus.app.data.loadBiology
import dk.jagtfokus.app.data.loadSpeciesPhotos
import dk.jagtfokus.app.location.AndroidLocationSource
import dk.jagtfokus.app.jagtbart.AllSpeciesScreen
import dk.jagtfokus.app.jagtbart.AppTab
import dk.jagtfokus.app.jagtbart.SettingsScreen
import dk.jagtfokus.app.jagtbart.WelcomeScreen
import dk.jagtfokus.app.jagtbart.BagStore
import dk.jagtfokus.app.jagtbart.JagtbartScreen
import dk.jagtfokus.app.jagtbart.JagtbartViewModel
import dk.jagtfokus.app.jagtbart.JournalScreen
import dk.jagtfokus.app.jagtbart.LocationState
import dk.jagtfokus.app.ui.appLocale
import dk.jagtfokus.core.SeasonCalendar
import dk.jagtfokus.app.ui.theme.Appearance
import dk.jagtfokus.app.ui.theme.BrandDeep
import dk.jagtfokus.app.ui.theme.JagtFokusTheme
import dk.jagtfokus.app.ui.theme.Spacing
import java.io.File
import java.time.LocalDate
import java.time.format.DateTimeParseException

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        // Bundled file, or the downloaded one when it is newer. Never the bundled one alone.
        val catalog = runCatching { CatalogRepository(applicationContext).load() }.getOrNull()
        val biology = runCatching { loadBiology(this) }.getOrNull()
        val photos = runCatching { loadSpeciesPhotos(this) }.getOrNull()
        // Same idea as the iOS launch arguments (`#if DEBUG` there): date, place and sheet. Debug builds only.
        val debuggable = applicationContext.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE != 0
        val date = if (debuggable) {
            intent.getStringExtra("date")?.let {
                try {
                    LocalDate.parse(it)
                } catch (_: DateTimeParseException) {
                    null
                }
            }
        } else {
            null
        }
        val placeId = if (debuggable) intent.getStringExtra("place") else null
        val sheet = if (debuggable) intent.getStringExtra("sheet") else null
        setContent {
            val prefs = getSharedPreferences("jagtapp", android.content.Context.MODE_PRIVATE)
            var welcomed by androidx.compose.runtime.remember {
                androidx.compose.runtime.mutableStateOf(prefs.getBoolean("velkomst", false))
            }
            if (!welcomed) {
                JagtFokusTheme(darkTheme = true) {
                    // Dark brand world: light icons whatever the system or Udseende says.
                    SystemBars(dark = true, background = BrandDeep)
                    Box(Modifier.fillMaxSize()) {
                        WelcomeScreen(onAccept = {
                            prefs.edit { putBoolean("velkomst", true) }
                            welcomed = true
                        })
                    }
                }
                return@setContent
            }
            var appearance by androidx.compose.runtime.remember {
                androidx.compose.runtime.mutableStateOf(
                    runCatching { Appearance.valueOf(prefs.getString("udseende", "SYSTEM") ?: "SYSTEM") }.getOrDefault(Appearance.SYSTEM),
                )
            }
            val darkTheme = when (appearance) {
                Appearance.SYSTEM -> isSystemInDarkTheme()
                Appearance.LIGHT -> false
                Appearance.DARK -> true
            }
            JagtFokusTheme(darkTheme = darkTheme) {
                SystemBars(dark = darkTheme, background = MaterialTheme.colorScheme.background)
                if (catalog == null) {
                    Box(Modifier.fillMaxSize().padding(Spacing.l), contentAlignment = Alignment.Center) {
                        Text(
                            stringResource(R.string.catalog_error),
                            style = MaterialTheme.typography.bodyLarge,
                        )
                    }
                } else {
                    val bag = (applicationContext as JagtFokusApp).bag
                    val vm = viewModel {
                        JagtbartViewModel(
                            catalog = catalog,
                            catalogs = CatalogRepository(applicationContext),
                            weatherClient = WeatherClient(),
                            location = AndroidLocationSource(applicationContext),
                            initialDate = date,
                            initialPlaceId = placeId,
                        )
                    }
                    val locale = appLocale()
                    val calendar = remember(locale) { SeasonCalendar(locale = locale) }
                    ShellLifecycle(vm, bag)
                    var tab by rememberSaveable { mutableStateOf(AppTab.Jagtbart) }
                    // Bumped when the open tab is tapped again; the screen pops to its root.
                    var reselect by remember { mutableIntStateOf(0) }
                    val tabStates = rememberSaveableStateHolder()
                    fun select(target: AppTab) {
                        if (target == tab) reselect++ else tab = target
                    }
                    // A swipe on a species row records it and switches to the journal.
                    LaunchedEffect(bag.tabRequests) {
                        if (bag.tabRequests > 0) tab = AppTab.Journal
                    }
                    // Registered before the screens, so a screen's own pushed page takes Back first.
                    BackHandler(tab != AppTab.Jagtbart) { tab = AppTab.Jagtbart }
                    // The IME pad is on the whole column: the keyboard lifts the bar and the content together.
                    Column(Modifier.fillMaxSize().imePadding()) {
                        // The bar below pads for the navigation bar, so the screens must not do it twice.
                        Box(
                            Modifier
                                .weight(1f)
                                .consumeWindowInsets(WindowInsets.navigationBars.only(WindowInsetsSides.Bottom)),
                        ) {
                            // Each tab keeps its page, scroll and query while another tab is open.
                            tabStates.SaveableStateProvider(tab.name) {
                                when (tab) {
                                    AppTab.Jagtbart -> JagtbartScreen(vm, biology, photos, bag, onSwitchToJournal = { tab = AppTab.Journal }, initialSheet = sheet, reselect = reselect)
                                    AppTab.Journal -> JournalScreen(vm, bag, calendar, reselect)
                                    AppTab.AllSpecies -> AllSpeciesScreen(vm, biology, photos, bag, onSwitchToJournal = { tab = AppTab.Journal }, reselect = reselect)
                                    AppTab.Settings -> SettingsScreen(vm, calendar, appearance, reselect) { option ->
                                        appearance = option
                                        prefs.edit { putString("udseende", option.name) }
                                    }
                                }
                            }
                        }
                        val navColors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                            selectedTextColor = MaterialTheme.colorScheme.onSurface,
                            indicatorColor = MaterialTheme.colorScheme.secondaryContainer,
                        )
                        NavigationBar {
                            NavigationBarItem(
                                selected = tab == AppTab.Jagtbart,
                                onClick = { select(AppTab.Jagtbart) },
                                icon = { Icon(Icons.Outlined.MyLocation, contentDescription = null) },
                                label = { NavLabel(stringResource(R.string.tab_jagtbart)) },
                                colors = navColors,
                            )
                            NavigationBarItem(
                                selected = tab == AppTab.Journal,
                                onClick = { select(AppTab.Journal) },
                                icon = { Icon(Icons.Outlined.MenuBook, contentDescription = null) },
                                label = { NavLabel(stringResource(R.string.journal_tab)) },
                                colors = navColors,
                            )
                            NavigationBarItem(
                                selected = tab == AppTab.AllSpecies,
                                onClick = { select(AppTab.AllSpecies) },
                                icon = { Icon(Icons.Outlined.FormatListBulleted, contentDescription = null) },
                                label = { NavLabel(stringResource(R.string.all_species_tab)) },
                                colors = navColors,
                            )
                            NavigationBarItem(
                                selected = tab == AppTab.Settings,
                                onClick = { select(AppTab.Settings) },
                                icon = { Icon(Icons.Outlined.Settings, contentDescription = null) },
                                label = { NavLabel(stringResource(R.string.settings_tab)) },
                                colors = navColors,
                            )
                        }
                    }
                }
            }
        }
    }
}

/** One line, never broken mid-word, even at 200 % font. The icon carries no description: the label is read. */
@Composable
private fun NavLabel(text: String) {
    Text(text, maxLines = 1, softWrap = false, overflow = TextOverflow.Ellipsis)
}

/**
 * System bars follow the app's Udseende, not the phone's: [dark] means the app is dark, so the
 * icons are light. Also tints the window behind the first frame and rotation.
 */
@Composable
private fun SystemBars(dark: Boolean, background: Color) {
    val activity = LocalActivity.current as? ComponentActivity ?: return
    val scrim = AndroidColor.argb(0x80, 0x1b, 0x1b, 0x1b)
    SideEffect {
        val bars = if (dark) {
            SystemBarStyle.dark(AndroidColor.TRANSPARENT)
        } else {
            SystemBarStyle.light(AndroidColor.TRANSPARENT, scrim)
        }
        activity.enableEdgeToEdge(statusBarStyle = bars, navigationBarStyle = bars)
        activity.window.setBackgroundDrawable(background.toArgb().toDrawable())
    }
}

/**
 * Runs whatever tab is open (iOS `RootView`): the start-up catalog check, a fresh "today" and
 * a fresh position on resume, and a fresh "today" when the clock, date or zone changes.
 */
@Composable
private fun ShellLifecycle(vm: JagtbartViewModel, bag: BagStore) {
    val context = LocalContext.current
    val locale by rememberUpdatedState(appLocale())

    LaunchedEffect(vm) { vm.checkCatalogSilently() }

    LifecycleResumeEffect(vm) {
        vm.onResume()
        val granted = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED
        if (granted) {
            if (vm.followHere || bag.draft.followHere) vm.locate(locale)
        } else if (vm.locationState != LocationState.Idle) {
            // Switched off in Settings meanwhile. Idle is the first start, before the dialog.
            vm.onDenied()
        }
        onPauseOrDispose {}
    }

    LifecycleStartEffect(vm) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) = vm.onResume()
        }
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_DATE_CHANGED)
            addAction(Intent.ACTION_TIME_CHANGED)
            addAction(Intent.ACTION_TIMEZONE_CHANGED)
        }
        ContextCompat.registerReceiver(context, receiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED)
        onStopOrDispose { context.unregisterReceiver(receiver) }
    }
}
