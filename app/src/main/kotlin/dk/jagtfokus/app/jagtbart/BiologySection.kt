package dk.jagtfokus.app.jagtbart

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.OpenInNew
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import dk.jagtfokus.app.R
import dk.jagtfokus.app.ui.theme.Spacing
import dk.jagtfokus.core.TaxonBiology

/**
 * Life-history notes on the species page. Below the hunting answer.
 * Display only. No logic may use the text.
 */
@Composable
fun BiologySection(biology: TaxonBiology) {
    Column {
        Text(
            stringResource(R.string.biology),
            style = MaterialTheme.typography.titleSmall,
            modifier = Modifier.padding(horizontal = Spacing.l, vertical = Spacing.s),
        )
        BiologyFact(stringResource(R.string.latin_name), biology.latin, italic = true)
        biology.facts.forEach { fact ->
            BiologyFact(fact.label, fact.value)
        }
        biology.sources.forEach { source ->
            val context = LocalContext.current
            ListItem(
                headlineContent = { Text(source.name, color = MaterialTheme.colorScheme.primary) },
                leadingContent = {
                    Icon(Icons.Outlined.OpenInNew, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                },
                modifier = Modifier
                    .clickable {
                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(source.url)))
                    },
            )
        }
        Text(
            stringResource(R.string.biology_disclaimer),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(Spacing.l),
        )
    }
}

@Composable
private fun BiologyFact(label: String, value: String, italic: Boolean = false) {
    Column(Modifier.padding(horizontal = Spacing.l, vertical = Spacing.xxs)) {
        Text(
            label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            value,
            style = MaterialTheme.typography.bodyLarge,
            fontStyle = if (italic) FontStyle.Italic else FontStyle.Normal,
        )
    }
}
