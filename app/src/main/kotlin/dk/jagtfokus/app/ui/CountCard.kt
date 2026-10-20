package dk.jagtfokus.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import dk.jagtfokus.app.ui.theme.LocalSeasonColors
import dk.jagtfokus.app.ui.theme.Radius
import dk.jagtfokus.app.ui.theme.Spacing

/**
 * A big number with a line of text beside it. The journal's year total, and, like iOS
 * `CountCard`, what Jagtbart's answer card can share later. One element for TalkBack.
 * [title] is the unit ("stykker"), so the number is not said twice.
 */
@Composable
fun CountCard(count: Int, title: String, detail: String? = null) {
    val open = LocalSeasonColors.current.open
    val spoken = listOfNotNull("$count $title", detail).joinToString(", ")
    Surface(
        modifier = Modifier.fillMaxWidth().clearAndSetSemantics { contentDescription = spoken },
        shape = RoundedCornerShape(Radius.large),
        color = open.copy(alpha = if (count > 0) 0.12f else 0.05f),
    ) {
        Row(
            Modifier.padding(Spacing.m),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.m),
        ) {
            Text(
                count.toString(),
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Bold,
                color = if (count > 0) open else MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium, overflow = TextOverflow.Ellipsis)
                if (detail != null) {
                    Text(detail, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}
