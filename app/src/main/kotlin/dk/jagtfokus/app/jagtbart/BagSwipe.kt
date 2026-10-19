package dk.jagtfokus.app.jagtbart

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AddCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import dk.jagtfokus.app.R
import dk.jagtfokus.app.ui.theme.LocalSeasonColors
import dk.jagtfokus.app.ui.theme.Spacing

/**
 * Swipe left on a species row records it in the journal and switches to the
 * Journal tab, like the trailing swipe action on iOS. The row snaps back:
 * the action is a note, not a deletion.
 */
@Composable
internal fun BagSwipe(onSwipeRecord: () -> Unit, content: @Composable () -> Unit) {
    val label = stringResource(R.string.journal_tab)
    val state = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            if (value == SwipeToDismissBoxValue.EndToStart) onSwipeRecord()
            false
        },
    )
    SwipeToDismissBox(
        state = state,
        enableDismissFromStartToEnd = false,
        backgroundContent = {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(LocalSeasonColors.current.open.copy(alpha = 0.12f))
                    .padding(horizontal = Spacing.l),
                contentAlignment = Alignment.CenterEnd,
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(Spacing.xs), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.AddCircle, contentDescription = null, tint = LocalSeasonColors.current.open)
                    Text(label, color = LocalSeasonColors.current.open, fontWeight = FontWeight.SemiBold)
                }
            }
        },
    ) {
        Box(Modifier.background(MaterialTheme.colorScheme.surfaceContainerLow)) {
            content()
        }
    }
}
