package dk.jagtfokus.app.jagtbart

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Place
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dk.jagtfokus.app.R
import dk.jagtfokus.app.ui.theme.BrandDeep
import dk.jagtfokus.app.ui.theme.BrandGold
import dk.jagtfokus.app.ui.theme.BrandGlow
import dk.jagtfokus.app.ui.theme.Radius
import dk.jagtfokus.app.ui.theme.Spacing

/**
 * First start. Explains the app and the disclaimer, and primes the location prompt.
 * "Forstået" is required before the list. Port of WelcomeView.swift.
 */
@Composable
fun WelcomeScreen(onAccept: () -> Unit) {
    Box(
        Modifier
            .fillMaxSize()
            .drawBehind {
                drawRect(BrandDeep)
                // Lighter green glow from the top trailing corner.
                drawRect(
                    Brush.radialGradient(
                        colors = listOf(BrandGlow.copy(alpha = 0.9f), Color.Transparent),
                        center = Offset(size.width, 0f),
                        radius = size.maxDimension * 1.1f,
                    ),
                )
            },
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Spacing.l),
        ) {
            Spacer(Modifier.height(Spacing.xl))
            Image(
                painter = painterResource(R.drawable.brand_mark),
                contentDescription = null,
                modifier = Modifier.size(76.dp),
            )
            Text(
                stringResource(R.string.welcome_title),
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                modifier = Modifier.padding(top = Spacing.m),
            )
            Text(
                stringResource(R.string.welcome_subtitle),
                style = MaterialTheme.typography.titleMedium,
                color = Color.White.copy(alpha = 0.8f),
                modifier = Modifier.padding(top = Spacing.xs),
            )
            Spacer(Modifier.height(Spacing.l))
            Feature(Icons.Outlined.CalendarMonth, stringResource(R.string.welcome_feature_date_title), stringResource(R.string.welcome_feature_date))
            Feature(Icons.Outlined.Place, stringResource(R.string.welcome_feature_local_title), stringResource(R.string.welcome_feature_local))
            Feature(Icons.Outlined.Security, stringResource(R.string.welcome_feature_privacy_title), stringResource(R.string.welcome_feature_privacy))
            Spacer(Modifier.height(Spacing.l))
            ImportantBox()
            Spacer(Modifier.height(Spacing.l))
        }
        Column(
            Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(BrandDeep.copy(alpha = 0f), BrandDeep),
                    ),
                )
                .padding(horizontal = Spacing.l, vertical = Spacing.s),
        ) {
            Button(
                onClick = onAccept,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(50),
                colors = ButtonDefaults.buttonColors(
                    containerColor = BrandGold,
                    contentColor = BrandDeep,
                ),
            ) {
                Text(
                    stringResource(R.string.welcome_accept),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(vertical = Spacing.xxs),
                )
            }
            Text(
                stringResource(R.string.welcome_after),
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.6f),
                modifier = Modifier.padding(top = Spacing.xs).align(Alignment.CenterHorizontally),
            )
        }
    }
}

/** One gold icon, a headline and a line. */
@Composable
private fun Feature(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, text: String) {
    Row(
        Modifier.padding(vertical = Spacing.s),
        horizontalArrangement = Arrangement.spacedBy(Spacing.m),
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = BrandGold,
            modifier = Modifier.size(24.dp),
        )
        Column {
            Text(title, style = MaterialTheme.typography.titleMedium, color = Color.White)
            Text(
                text,
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.75f),
            )
        }
    }
}

/** The disclaimer in a gold-bordered box. */
@Composable
private fun ImportantBox() {
    Column(
        Modifier
            .fillMaxWidth()
            .background(Color.White.copy(alpha = 0.07f), RoundedCornerShape(Radius.large))
            .border(1.dp, BrandGold.copy(alpha = 0.35f), RoundedCornerShape(Radius.large))
            .padding(Spacing.m),
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Outlined.Warning, contentDescription = null, tint = BrandGold, modifier = Modifier.size(18.dp))
            Text(
                stringResource(R.string.welcome_important),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = BrandGold,
            )
        }
        Text(
            stringResource(R.string.disclaimer),
            style = MaterialTheme.typography.bodyMedium,
            color = Color.White.copy(alpha = 0.85f),
            modifier = Modifier.padding(top = Spacing.xs),
        )
        Text(
            stringResource(R.string.daylight),
            style = MaterialTheme.typography.bodyMedium,
            color = Color.White.copy(alpha = 0.85f),
        )
    }
}
