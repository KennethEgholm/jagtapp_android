package dk.jagtfokus.app.ui

import androidx.compose.foundation.clickable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.OpenInNew
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler

/**
 * One row that opens a web page. Opening is wrapped: a phone without a
 * browser must not crash the app, the tap just does nothing.
 */
@Composable
fun LinkRow(text: String, url: String, modifier: Modifier = Modifier) {
    val uriHandler = LocalUriHandler.current
    ListItem(
        headlineContent = { Text(text, color = MaterialTheme.colorScheme.primary) },
        leadingContent = {
            Icon(Icons.AutoMirrored.Outlined.OpenInNew, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        },
        modifier = modifier.clickable { runCatching { uriHandler.openUri(url) } },
    )
}
