package dk.jagtfokus.app.ui

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.MyLocation
import androidx.compose.material.icons.outlined.Place
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import dk.jagtfokus.app.R
import dk.jagtfokus.app.jagtbart.LocationState
import dk.jagtfokus.app.ui.theme.Spacing
import dk.jagtfokus.core.Place
import dk.jagtfokus.core.SeasonCalendar
import java.time.LocalDate

/**
 * The date and place card of Jagtbart and Journal. The caller owns the state: Jagtbart passes
 * its query, Journal its own draft. [followHere] shows the position icon and the "Din position"
 * detail, and, with [showLocationProblem], the footer that says why the place is not the GPS one.
 */
@Composable
fun DatePlaceCard(
    calendar: SeasonCalendar,
    date: LocalDate,
    today: LocalDate,
    place: Place,
    followHere: Boolean,
    locationState: LocationState,
    gpsPlace: Place?,
    onDate: () -> Unit,
    onPlace: () -> Unit,
    showLocationProblem: Boolean = true,
    dateHint: String = stringResource(R.string.choose_another_day),
    placeHint: String = stringResource(R.string.choose_another_place),
) {
    val dateValue = calendar.longDate(date).capitalizedFirst(calendar.locale)
    val dateDetail = if (date == today) stringResource(R.string.today) else null
    val placeDetail = when {
        !followHere -> null
        locationState == LocationState.Locating -> stringResource(R.string.finding_position)
        locationState == LocationState.Found -> stringResource(R.string.your_position)
        else -> null
    }
    val problem = if (showLocationProblem) locationProblem(followHere, locationState, gpsPlace, place) else null
    SectionCard {
        Column {
            QueryRow(
                icon = Icons.Outlined.CalendarMonth,
                title = stringResource(R.string.date),
                value = dateValue,
                detail = dateDetail,
                hint = dateHint,
                onClick = onDate,
            )
            QueryRow(
                icon = if (followHere) Icons.Outlined.MyLocation else Icons.Outlined.Place,
                title = stringResource(R.string.place),
                value = place.name,
                detail = placeDetail,
                hint = placeHint,
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
