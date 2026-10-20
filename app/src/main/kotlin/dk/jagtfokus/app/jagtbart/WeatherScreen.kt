package dk.jagtfokus.app.jagtbart

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Air
import androidx.compose.material.icons.outlined.DeviceThermostat
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.material.icons.outlined.WaterDrop
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import dk.jagtfokus.app.R
import dk.jagtfokus.app.ui.LabeledValueRow
import dk.jagtfokus.app.ui.MoonPhaseIcon
import dk.jagtfokus.app.ui.SunEventIcon
import dk.jagtfokus.app.ui.decimal
import dk.jagtfokus.app.ui.moonWords
import dk.jagtfokus.app.ui.percent
import dk.jagtfokus.app.ui.theme.LocalSeasonColors
import dk.jagtfokus.app.ui.theme.Spacing
import dk.jagtfokus.app.ui.weatherIcon
import dk.jagtfokus.app.ui.weatherWords
import dk.jagtfokus.app.ui.windFrom
import dk.jagtfokus.core.MetDay
import dk.jagtfokus.core.MoonPhase
import dk.jagtfokus.core.SeasonCalendar
import dk.jagtfokus.core.SunTimes
import dk.jagtfokus.core.clockTime
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WeatherScreen(
    calendar: SeasonCalendar,
    place: String,
    date: LocalDate,
    isToday: Boolean,
    sun: SunTimes?,
    moon: MoonPhase,
    weather: MetDay?,
    weatherUi: WeatherUi,
    onBack: () -> Unit,
    /** Called when the screen opens, so the host can retry a failed forecast. */
    onAppear: () -> Unit = {},
) {
    LaunchedEffect(Unit) { onAppear() }
    val basis = basisText(place, calendar.full(calendar.monthDay(date)))
    val scroll = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    Scaffold(
        modifier = Modifier.nestedScroll(scroll.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
                title = { Text(stringResource(R.string.sun_weather)) },
                scrollBehavior = scroll,
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
            )
        },
    ) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding)) {
            item {
                Text(
                    basis,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = Spacing.l, vertical = Spacing.xs),
                )
            }
            when (weatherUi) {
                WeatherUi.Loading, WeatherUi.Idle -> item { LoadingRow() }
                WeatherUi.Failed -> item { NoteRow(stringResource(R.string.weather_failed)) }
                WeatherUi.Unavailable -> item { NoteRow(stringResource(R.string.weather_horizon)) }
                WeatherUi.Loaded -> if (weather != null) {
                    item { SectionLabel(if (isToday) R.string.rest_of_day else R.string.whole_day) }
                    item { DaySection(weather, calendar) }
                    item { SectionLabel(if (isToday) R.string.now else R.string.midday) }
                    item { HourSection(weather, calendar) }
                }
            }
            item { SectionLabel(R.string.sun_and_moon) }
            item {
                if (sun != null) {
                    FactRow(stringResource(R.string.sunrise), sun.sunrise?.let(calendar::clockTime) ?: "–") {
                        SunEventIcon(rising = true, modifier = Modifier.size(18.dp))
                    }
                    FactRow(stringResource(R.string.sunset), sun.sunset?.let(calendar::clockTime) ?: "–") {
                        SunEventIcon(rising = false, modifier = Modifier.size(18.dp))
                    }
                }
                FactRow(
                    stringResource(R.string.moon),
                    stringResource(moonWords(moon.name)),
                    note = stringResource(R.string.lit, percent(moon.illumination, calendar.locale)),
                ) {
                    MoonPhaseIcon(moon.name, Modifier.size(18.dp))
                }
                Text(
                    stringResource(R.string.weather_source),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(Spacing.l),
                )
            }
        }
    }
}

@Composable
private fun DaySection(day: MetDay, calendar: SeasonCalendar) {
    val words = weatherWords(day.symbolCode)
    if (words != null) {
        FactRow(stringResource(R.string.weather), stringResource(words)) {
            Icon(weatherIcon(day.symbolCode), contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
    val max = day.maxTemperature
    val min = day.minTemperature
    if (max != null && min != null) {
        FactRow(
            stringResource(R.string.temperature),
            stringResource(R.string.max_min, decimal(max, calendar.locale), decimal(min, calendar.locale)),
        ) {
            Icon(Icons.Outlined.DeviceThermostat, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
    day.precipitation?.let {
        FactRow(stringResource(R.string.precipitation), decimal(it, calendar.locale, 0, 1) + " mm") {
            Icon(Icons.Outlined.WaterDrop, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
    wind(day.maxWindSpeed, day.maxWindDirection, calendar)?.let {
        FactRow(stringResource(R.string.strongest_wind), it) {
            WindLeading(day.maxWindDirection)
        }
    }
}

@Composable
private fun HourSection(day: MetDay, calendar: SeasonCalendar) {
    day.temperature?.let {
        FactRow(stringResource(R.string.temperature), decimal(it, calendar.locale) + "°") {
            Icon(Icons.Outlined.DeviceThermostat, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
    wind(day.windSpeed, day.windDirection, calendar)?.let {
        FactRow(stringResource(R.string.wind), it) {
            WindLeading(day.windDirection)
        }
    }
    day.humidity?.let {
        FactRow(stringResource(R.string.humidity), percent(it / 100, calendar.locale)) {
            Icon(Icons.Outlined.WaterDrop, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
    day.pressure?.let {
        FactRow(
            stringResource(R.string.pressure),
            decimal(it, calendar.locale, 0, 0) + " hPa",
            note = pressureTrend(day.pressureTrend),
        ) {
            Icon(Icons.Outlined.Speed, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun wind(speed: Double?, direction: Double?, calendar: SeasonCalendar): String? {
    speed ?: return null
    val value = decimal(speed, calendar.locale)
    if (direction == null) return "$value m/s"
    return stringResource(R.string.wind_from, value, stringResource(windFrom(direction)))
}

@Composable
private fun pressureTrend(trend: Double?): String? = when {
    trend == null -> null
    trend > 0.3 -> stringResource(R.string.pressure_rising)
    trend < -0.3 -> stringResource(R.string.pressure_falling)
    else -> stringResource(R.string.pressure_steady)
}

@Composable
private fun FactRow(
    title: String,
    value: String,
    note: String? = null,
    leading: @Composable () -> Unit,
) {
    LabeledValueRow(
        label = title,
        value = value,
        note = note,
        valueStyle = MaterialTheme.typography.bodyLarge.copy(fontFeatureSettings = "tnum"),
        leading = { Box(Modifier.size(24.dp), contentAlignment = Alignment.Center) { leading() } },
    )
}

/** The compass arrow when MET gives a direction, else plain wind. */
@Composable
private fun WindLeading(direction: Double?) {
    if (direction != null) {
        WindArrow(direction, Modifier.size(18.dp))
    } else {
        Icon(Icons.Outlined.Air, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
    }
}

@Composable
fun WindArrow(fromDegrees: Double, modifier: Modifier = Modifier) {
    val color = MaterialTheme.colorScheme.onSurfaceVariant
    Canvas(modifier.size(18.dp)) {
        val stroke = 2.dp.toPx()
        val wing = 5.dp.toPx()
        val center = Offset(size.width / 2, size.height / 2)
        // Points the way the wind blows: wind from the east points west, as on weather maps.
        rotate(fromDegrees.toFloat(), center) {
            drawLine(color, Offset(center.x, size.height * 0.15f), Offset(center.x, size.height * 0.85f), stroke, StrokeCap.Round)
            drawLine(color, Offset(center.x, size.height * 0.85f), Offset(center.x - wing, size.height * 0.62f), stroke, StrokeCap.Round)
            drawLine(color, Offset(center.x, size.height * 0.85f), Offset(center.x + wing, size.height * 0.62f), stroke, StrokeCap.Round)
        }
    }
}

@Composable
private fun SectionLabel(text: Int) {
    Text(
        stringResource(text),
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(horizontal = Spacing.l, vertical = Spacing.s).semantics { heading() },
    )
}

@Composable
private fun LoadingRow() {
    Row(Modifier.padding(Spacing.l), horizontalArrangement = Arrangement.spacedBy(Spacing.s), verticalAlignment = Alignment.CenterVertically) {
        CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
        Text(stringResource(R.string.weather_loading), color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun NoteRow(text: String) {
    Text(text, Modifier.padding(Spacing.l), color = MaterialTheme.colorScheme.onSurfaceVariant)
}

@Composable
fun basisText(place: String, day: String): String = stringResource(R.string.basis, place, day)
