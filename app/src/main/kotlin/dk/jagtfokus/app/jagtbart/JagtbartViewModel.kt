package dk.jagtfokus.app.jagtbart

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dk.jagtfokus.app.data.CatalogRepository
import dk.jagtfokus.app.data.RemoteOutcome
import dk.jagtfokus.app.data.WeatherClient
import dk.jagtfokus.core.HuntingCatalog
import dk.jagtfokus.core.MetDay
import dk.jagtfokus.core.Place
import dk.jagtfokus.core.SeasonCalendar
import dk.jagtfokus.core.denmark
import dk.jagtfokus.core.matchPlace
import dk.jagtfokus.core.place
import dk.jagtfokus.app.location.AndroidLocationSource
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.util.Locale

enum class WeatherUi { Idle, Loading, Loaded, Failed, Unavailable }

enum class LocationState {
    Idle,
    Locating,
    Found,
    Denied,
    Unavailable,
}

/**
 * The last time the remote catalog was asked, and what came of it. iOS keeps only the
 * outcome text (`remoteStatus`); [at] lets Indstillinger say when.
 */
data class CatalogCheck(val outcome: RemoteOutcome, val at: Instant)

/**
 * Date and place for Jagtbart. The list itself is computed by [SeasonCalendar]
 * in the screen, so the view only formats.
 */
class JagtbartViewModel(
    catalog: HuntingCatalog,
    private val catalogs: CatalogRepository,
    private val weatherClient: WeatherClient,
    private val location: AndroidLocationSource,
    initialDate: LocalDate?,
    initialPlaceId: String?,
) : ViewModel() {
    var catalog by mutableStateOf(catalog)
        private set
    var now by mutableStateOf(LocalDate.now(SeasonCalendar.ZONE))
        private set
    var followToday by mutableStateOf(initialDate == null)
        private set
    var followHere by mutableStateOf(initialPlaceId == null)
        private set
    var pickedDate by mutableStateOf(initialDate ?: now)
        private set
    var pickedPlace by mutableStateOf(catalog.place(initialPlaceId ?: "") ?: catalog.denmark)
        private set
    var gpsPlace by mutableStateOf<Place?>(null)
        private set
    var gpsLatitude by mutableStateOf<Double?>(null)
        private set
    var gpsLongitude by mutableStateOf<Double?>(null)
        private set
    private var gpsNames: List<String> = emptyList()
    var locationState by mutableStateOf(LocationState.Idle)
        private set
    var refreshing by mutableStateOf(false)
        private set
    var expanded by mutableStateOf(setOf<String>())
        private set
    var showsClosed by mutableStateOf(false)
        private set
    var weather by mutableStateOf<MetDay?>(null)
        private set
    var weatherUi by mutableStateOf(WeatherUi.Idle)
        private set

    /**
     * Result of the latest remote catalog check, from the silent check at start or from pull
     * to refresh. Null until the first check has finished. Shown in Indstillinger as "Data".
     */
    var lastCheck by mutableStateOf<CatalogCheck?>(null)
        private set

    val queryDate: LocalDate
        get() = if (followToday) now else pickedDate

    val queryPlace: Place
        get() = when {
            followHere && gpsPlace != null -> gpsPlace!!
            followHere -> catalog.denmark
            else -> pickedPlace
        }

    private var locateJob: Job? = null
    private var silentCheckStarted = false

    fun onResume() {
        now = LocalDate.now(SeasonCalendar.ZONE)
    }

    fun select(date: LocalDate) {
        if (date == LocalDate.now(SeasonCalendar.ZONE)) {
            followToday = true
            now = date
            pickedDate = date
        } else {
            followToday = false
            pickedDate = date
        }
    }

    fun select(place: Place) {
        followHere = false
        pickedPlace = place
    }

    fun useHere() {
        followHere = true
    }

    fun toggleGroup(id: String) {
        expanded = if (id in expanded) expanded - id else expanded + id
    }

    fun toggleClosed() {
        showsClosed = !showsClosed
    }

    fun loadWeather(latitude: Double, longitude: Double, on: LocalDate) {
        viewModelScope.launch {
            weatherUi = WeatherUi.Loading
            val result = weatherClient.load(latitude, longitude, on, SeasonCalendar.ZONE, java.time.Instant.now())
            result.fold(
                onSuccess = { day ->
                    weather = day
                    weatherUi = if (day == null) WeatherUi.Unavailable else WeatherUi.Loaded
                },
                onFailure = {
                    weather = null
                    weatherUi = WeatherUi.Failed
                },
            )
        }
    }

    /** Position is off. Drop the last island, so the list does not keep showing it. */
    fun onDenied() {
        locateJob?.cancel()
        gpsPlace = null
        gpsLatitude = null
        gpsLongitude = null
        gpsNames = emptyList()
        locationState = LocationState.Denied
        refreshing = false
    }

    fun locate(locale: Locale, indicator: Boolean = false) {
        if (locateJob?.isActive == true && !indicator) return
        locateJob?.cancel()
        locateJob = viewModelScope.launch {
            if (indicator) refreshing = true
            if (gpsPlace == null) locationState = LocationState.Locating
            try {
                val fix = location.fix(locale)
                gpsLatitude = fix.latitude
                gpsLongitude = fix.longitude
                gpsNames = fix.names
                gpsPlace = catalog.matchPlace(fix.names)
                locationState = LocationState.Found
            } catch (_: Exception) {
                locationState = LocationState.Unavailable
            } finally {
                if (indicator) refreshing = false
            }
        }
    }

    /**
     * Asks the remote catalog once per process, without spinner or snackbar. iOS does the same
     * in `SeasonStore.init`. [lastCheck] records the outcome.
     */
    fun checkCatalogSilently() {
        if (silentCheckStarted) return
        silentCheckStarted = true
        viewModelScope.launch { fetchCatalog() }
    }

    /** Pull to refresh the catalog. Location is requested by the screen when it is allowed. */
    fun refreshCatalog(onOutcome: (RemoteOutcome) -> Unit) {
        refreshing = true
        viewModelScope.launch {
            val outcome = fetchCatalog()
            refreshing = false
            onOutcome(outcome)
        }
    }

    private suspend fun fetchCatalog(): RemoteOutcome {
        val (outcome, next) = catalogs.refresh(catalog)
        if (outcome == RemoteOutcome.Updated) {
            catalog = next
            pickedPlace = catalog.place(pickedPlace.id) ?: catalog.denmark
            if (gpsNames.isNotEmpty()) gpsPlace = catalog.matchPlace(gpsNames)
        }
        lastCheck = CatalogCheck(outcome, Instant.now())
        return outcome
    }
}
