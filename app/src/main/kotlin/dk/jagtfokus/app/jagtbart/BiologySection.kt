package dk.jagtfokus.app.jagtbart

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontStyle
import dk.jagtfokus.app.R
import dk.jagtfokus.app.ui.LinkRow
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
            modifier = Modifier.padding(horizontal = Spacing.l, vertical = Spacing.s).semantics { heading() },
        )
        BiologyFact(stringResource(R.string.latin_name), biology.latin, italic = true)
        biology.facts.forEach { fact ->
            BiologyFact(fact.label, fact.value)
        }
        biology.sources.forEach { source ->
            LinkRow(source.name, source.url)
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
