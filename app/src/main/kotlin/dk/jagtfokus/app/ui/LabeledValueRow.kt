package dk.jagtfokus.app.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle

/**
 * Label on the left, value (and an optional note under it) on the right.
 * From a font scale of 1.3 the value moves under the label so neither is
 * squeezed. One spoken item per row.
 */
@Composable
fun LabeledValueRow(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    note: String? = null,
    noteColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    labelColor: Color = Color.Unspecified,
    valueStyle: TextStyle = MaterialTheme.typography.bodyLarge,
    leading: (@Composable () -> Unit)? = null,
) {
    val stacked = LocalDensity.current.fontScale >= 1.3f
    val valueAndNote: @Composable (Alignment.Horizontal) -> Unit = { align ->
        Column(horizontalAlignment = align) {
            Text(value, style = valueStyle)
            if (note != null) Text(note, style = MaterialTheme.typography.bodySmall, color = noteColor)
        }
    }
    ListItem(
        headlineContent = { Text(label, color = labelColor) },
        supportingContent = if (stacked) ({ valueAndNote(Alignment.Start) }) else null,
        trailingContent = if (stacked) null else ({ valueAndNote(Alignment.End) }),
        leadingContent = leading,
        modifier = modifier.semantics(mergeDescendants = true) {},
    )
}
