package dk.jagtfokus.app.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ExpandLess
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.HourglassTop
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import dk.jagtfokus.app.R
import dk.jagtfokus.app.ui.theme.LocalSeasonColors
import dk.jagtfokus.app.ui.theme.Spacing

/**
 * A card with a fold header and its rows inside, like iOS' group sections. Jagtbart's open
 * groups and "Ikke jagtbar her" and Alle arter use it. [rows] show only when [expanded].
 */
@Composable
fun GroupCard(
    title: String,
    count: Int,
    leading: @Composable () -> Unit,
    expanded: Boolean,
    canFold: Boolean,
    foldHint: String,
    onToggle: () -> Unit,
    closingSoon: Int = 0,
    rows: @Composable () -> Unit,
) {
    SectionCard {
        Column {
            GroupHeader(title, count, leading, expanded, canFold, foldHint, onToggle, closingSoon)
            if (expanded) rows()
        }
    }
}

/**
 * Header of a [GroupCard]: icon, name, an optional "last day soon" warning, the count and a
 * chevron when the group can fold. TalkBack gets one heading that says name, count and warning,
 * and, when it folds, a button with Vist or Skjult (iOS `GroupHeader`, `FoldButton`).
 */
@Composable
fun GroupHeader(
    title: String,
    count: Int,
    leading: @Composable () -> Unit,
    expanded: Boolean,
    canFold: Boolean,
    foldHint: String,
    onToggle: () -> Unit,
    closingSoon: Int = 0,
) {
    val soonText = if (closingSoon > 0) pluralStringResource(R.plurals.closing_soon, closingSoon, closingSoon) else null
    val spoken = listOfNotNull(title, pluralStringResource(R.plurals.species_count, count, count), soonText)
        .joinToString(", ")
    val state = stringResource(if (expanded) R.string.shown else R.string.hidden)
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .then(
                if (canFold) Modifier.clickable(onClickLabel = foldHint, role = Role.Button, onClick = onToggle) else Modifier,
            )
            .padding(horizontal = Spacing.m, vertical = Spacing.s)
            .clearAndSetSemantics {
                heading()
                contentDescription = spoken
                if (canFold) stateDescription = state
            },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.s),
    ) {
        leading()
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            if (soonText != null) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.xxs)) {
                    Icon(
                        Icons.Outlined.HourglassTop,
                        contentDescription = null,
                        tint = LocalSeasonColors.current.closingSoon,
                        modifier = Modifier.size(14.dp),
                    )
                    Text(soonText, style = MaterialTheme.typography.bodySmall, color = LocalSeasonColors.current.closingSoon)
                }
            }
        }
        Text(
            count.toString(),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (canFold) {
            Icon(if (expanded) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore, contentDescription = null)
        }
    }
}
