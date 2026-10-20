package dk.jagtfokus.app.jagtbart

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Call
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.OpenInNew
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.core.net.toUri
import dk.jagtfokus.app.R
import dk.jagtfokus.app.ui.cardListItemColors
import dk.jagtfokus.app.ui.theme.Spacing

/**
 * Info page for the official Schweiss register. No handler phone list:
 * that waits for permission from the secretariat. Views only format.
 * Om pushes the same content once it exists.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SchweissSheet(onDismiss: () -> Unit) {
    // Tall sheet like iOS: the register is a whole info page, not a half sheet.
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = Spacing.l),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                stringResource(R.string.schweiss_title),
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.weight(1f),
            )
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.close)) }
        }
        Column(
            Modifier
                .verticalScroll(rememberScrollState())
                .padding(bottom = Spacing.l),
        ) {
            SchweissContent()
        }
    }
}

@Composable
fun SchweissContent() {
    SchweissSection(
        heading = stringResource(R.string.schweiss_hunt_heading),
        footer = stringResource(R.string.schweiss_hunt_footer),
    ) {
        Text(
            stringResource(R.string.schweiss_hunt_duty),
            Modifier.padding(horizontal = Spacing.l, vertical = Spacing.s),
        )
        SiteLink(stringResource(R.string.schweiss_find_handler), SchweissURL.handlers, hint = stringResource(R.string.schweiss_site_hint))
        SiteLink(stringResource(R.string.schweiss_source), SchweissURL.source, hint = stringResource(R.string.schweiss_source_hint))
    }
    SchweissSection(
        heading = stringResource(R.string.schweiss_traffic_heading),
        footer = stringResource(R.string.schweiss_traffic_footer),
    ) {
        CallLink(
            title = stringResource(R.string.schweiss_call_1812),
            url = SchweissURL.vagtcentral,
            label = stringResource(R.string.schweiss_call_1812_label),
        )
    }
    SchweissSection(heading = stringResource(R.string.schweiss_about_heading)) {
        Text(stringResource(R.string.schweiss_about), Modifier.padding(horizontal = Spacing.l, vertical = Spacing.s))
        Text(stringResource(R.string.schweiss_referral), Modifier.padding(horizontal = Spacing.l, vertical = Spacing.s))
        SiteLink(stringResource(R.string.schweiss_about_link), SchweissURL.about, hint = stringResource(R.string.schweiss_site_hint))
        SiteLink(stringResource(R.string.schweiss_rules), SchweissURL.rules, hint = stringResource(R.string.schweiss_site_hint))
        SiteLink(stringResource(R.string.schweiss_expect), SchweissURL.expect, hint = stringResource(R.string.schweiss_site_hint))
    }
    SchweissSection(
        heading = stringResource(R.string.schweiss_secretariat_heading),
        footer = stringResource(R.string.schweiss_secretariat_footer),
    ) {
        CallLink(
            title = stringResource(R.string.schweiss_call_secretariat),
            url = SchweissURL.secretariat,
            label = stringResource(R.string.schweiss_call_secretariat_label),
        )
        SiteLink(
            SchweissURL.secretariatMail,
            SchweissURL.mail,
            icon = Icons.Outlined.Email,
            hint = stringResource(R.string.schweiss_mail_hint),
        )
    }
}

/** One grouped section: heading on top, optional footer under, like the iOS List sections. */
@Composable
private fun SchweissSection(
    heading: String,
    footer: String? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column {
        Text(
            heading,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = Spacing.l, vertical = Spacing.s).semantics { heading() },
        )
        content()
        if (footer != null) {
            Text(
                footer,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(Spacing.l),
            )
        }
    }
}

/** A link row to a web page. The hint says what opening does. */
@Composable
private fun SiteLink(
    title: String,
    url: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector = Icons.Outlined.OpenInNew,
    hint: String? = null,
) {
    val context = LocalContext.current
    ListItem(
        headlineContent = { Text(title, color = MaterialTheme.colorScheme.primary) },
        leadingContent = { Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
        colors = cardListItemColors(),
        modifier = Modifier
            .clickable(role = Role.Button) { open(context, url) }
            .semantics {
                contentDescription = listOfNotNull(title, hint).joinToString(". ")
            },
    )
}

/** A phone number row. The long label replaces the short title for screen readers. */
@Composable
private fun CallLink(title: String, url: String, label: String) {
    val context = LocalContext.current
    ListItem(
        headlineContent = { Text(title, color = MaterialTheme.colorScheme.primary) },
        leadingContent = { Icon(Icons.Outlined.Call, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
        colors = cardListItemColors(),
        modifier = Modifier
            .clickable(role = Role.Button) { open(context, url) }
            .semantics { contentDescription = label },
    )
}

/** Dial and mail open their own apps; the web opens the browser. */
private fun open(context: Context, url: String) {
    val intent = when {
        url.startsWith("tel:") -> Intent(Intent.ACTION_DIAL, url.toUri())
        url.startsWith("mailto:") -> Intent(Intent.ACTION_SENDTO, url.toUri())
        else -> Intent(Intent.ACTION_VIEW, url.toUri())
    }
    runCatching { context.startActivity(intent) }
}

/** The register's own pages and numbers. The mail address is the same in every language. */
private object SchweissURL {
    val handlers = "https://schweiss.dk/hundeforere"
    val source = "https://naturstyrelsen.dk/aktiviteter-i-naturen/vildtforvaltning/tilskadekommet-vildt/schweisshunde"
    val about = "https://schweiss.dk/om-registret"
    val rules = "https://schweiss.dk/regler"
    val expect = "https://schweiss.dk/hvad-kan-jeg-forvente-af-en-legitimeret-schweisshundefoerer"
    const val vagtcentral = "tel:1812"
    const val secretariat = "tel:93597080"
    const val mail = "mailto:schweiss@nst.dk"

    /** An address, the same in every language. */
    const val secretariatMail = "schweiss@nst.dk"
}
