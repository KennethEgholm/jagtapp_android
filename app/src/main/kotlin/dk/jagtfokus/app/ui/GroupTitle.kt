package dk.jagtfokus.app.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import dk.jagtfokus.app.R

/** Group headings come from the data in Danish. These are the app's own names for
 *  the same groups. An unknown id falls back to the data title. */
@Composable
fun groupTitle(id: String, fromData: String): String = when (id) {
    "klovbaerende" -> stringResource(R.string.group_klovbaerende)
    "rovdyr" -> stringResource(R.string.group_rovdyr)
    "ovrige_pattedyr" -> stringResource(R.string.group_ovrige_pattedyr)
    "andefugle" -> stringResource(R.string.group_andefugle)
    "gaes" -> stringResource(R.string.group_gaes)
    "honsefugle" -> stringResource(R.string.group_honsefugle)
    "ovrige_fugle" -> stringResource(R.string.group_ovrige_fugle)
    else -> fromData
}
