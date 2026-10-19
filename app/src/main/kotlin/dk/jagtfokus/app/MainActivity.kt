package dk.jagtfokus.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewmodel.compose.viewModel
import dk.jagtfokus.app.data.CatalogRepository
import dk.jagtfokus.app.data.WeatherClient
import dk.jagtfokus.app.data.loadBiology
import dk.jagtfokus.app.data.loadCatalog
import dk.jagtfokus.app.data.loadSpeciesPhotos
import dk.jagtfokus.app.location.AndroidLocationSource
import dk.jagtfokus.app.jagtbart.AllSpeciesScreen
import dk.jagtfokus.app.jagtbart.AppTab
import dk.jagtfokus.app.jagtbart.SettingsScreen
import dk.jagtfokus.app.jagtbart.BagStore
import dk.jagtfokus.app.jagtbart.JagtbartScreen
import dk.jagtfokus.app.jagtbart.JagtbartViewModel
import dk.jagtfokus.app.jagtbart.JournalScreen
import dk.jagtfokus.app.ui.theme.Appearance
import dk.jagtfokus.app.ui.theme.JagtFokusTheme
import dk.jagtfokus.app.ui.theme.Spacing
import java.io.File
import java.time.LocalDate
import java.time.format.DateTimeParseException

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        val catalog = runCatching { loadCatalog(this) }.getOrNull()
        val biology = runCatching { loadBiology(this) }.getOrNull()
        val photos = runCatching { loadSpeciesPhotos(this) }.getOrNull()
        val date = intent.getStringExtra("date")?.let {
            try {
                LocalDate.parse(it)
            } catch (_: DateTimeParseException) {
                null
            }
        }
        val placeId = intent.getStringExtra("place")
        // Same idea as the iOS launch arguments: `-sheet schweiss` opens the sheet. Debug only.
        val debuggable = applicationContext.applicationInfo.flags and
            android.content.pm.ApplicationInfo.FLAG_DEBUGGABLE != 0
        val sheet = if (debuggable) intent.getStringExtra("sheet") else null
        setContent {
            val prefs = getSharedPreferences("jagtapp", android.content.Context.MODE_PRIVATE)
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
                if (catalog == null) {
                    Box(Modifier.fillMaxSize().padding(Spacing.l), contentAlignment = Alignment.Center) {
                        Text(
                            stringResource(R.string.catalog_error),
                            style = MaterialTheme.typography.bodyLarge,
                        )
                    }
                } else {
                    val bag = remember { BagStore(applicationContext.filesDir) }
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
                    val calendar = remember { dk.jagtfokus.core.SeasonCalendar() }
                    val tab = remember { androidx.compose.runtime.mutableStateOf(AppTab.Jagtbart) }
                    // A swipe on a species row records it and switches to the journal.
                    androidx.compose.runtime.LaunchedEffect(bag.tabRequests) {
                        if (bag.tabRequests > 0) tab.value = AppTab.Journal
                    }
                    Column(Modifier.fillMaxSize()) {
                        Box(Modifier.weight(1f)) {
                            when (tab.value) {
                                AppTab.Jagtbart -> JagtbartScreen(vm, biology, photos, bag, onSwitchToJournal = { tab.value = AppTab.Journal }, initialSheet = sheet)
                                AppTab.Journal -> JournalScreen(vm, bag, calendar)
                                AppTab.AllSpecies -> AllSpeciesScreen(vm, biology, photos, bag, onSwitchToJournal = { tab.value = AppTab.Journal })
                                AppTab.Settings -> SettingsScreen(vm, calendar, appearance) { option ->
                                    appearance = option
                                    prefs.edit().putString("udseende", option.name).apply()
                                }
                            }
                        }
                        NavigationBar {
                            NavigationBarItem(
                                selected = tab.value == AppTab.Jagtbart,
                                onClick = { tab.value = AppTab.Jagtbart },
                                icon = { Icon(Icons.Outlined.MyLocation, contentDescription = null) },
                                label = { Text(stringResource(R.string.tab_jagtbart)) },
                            )
                            NavigationBarItem(
                                selected = tab.value == AppTab.Journal,
                                onClick = { tab.value = AppTab.Journal },
                                icon = { Icon(Icons.Outlined.MenuBook, contentDescription = null) },
                                label = { Text(stringResource(R.string.journal_tab)) },
                            )
                            NavigationBarItem(
                                selected = tab.value == AppTab.AllSpecies,
                                onClick = { tab.value = AppTab.AllSpecies },
                                icon = { Icon(Icons.Outlined.FormatListBulleted, contentDescription = null) },
                                label = { Text(stringResource(R.string.all_species_tab)) },
                            )
                            NavigationBarItem(
                                selected = tab.value == AppTab.Settings,
                                onClick = { tab.value = AppTab.Settings },
                                icon = { Icon(Icons.Outlined.Settings, contentDescription = null) },
                                label = { Text(stringResource(R.string.settings_tab)) },
                            )
                        }
                    }
                }
            }
        }
    }
}
