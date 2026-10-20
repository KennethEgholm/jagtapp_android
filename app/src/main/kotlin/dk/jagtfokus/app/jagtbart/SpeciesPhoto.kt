package dk.jagtfokus.app.jagtbart

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dk.jagtfokus.app.R
import dk.jagtfokus.app.ui.theme.Radius
import dk.jagtfokus.app.ui.theme.Spacing
import dk.jagtfokus.core.IdentifiedPhoto
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Photo card with the credit line the license requires.
 * Renders nothing if the asset is missing, e.g. a species added by newer remote data.
 */
@Composable
fun SpeciesPhotoCard(photo: IdentifiedPhoto, speciesName: String, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val image = produceState<android.graphics.Bitmap?>(initialValue = null, photo.id) {
        value = withContext(Dispatchers.IO) {
            runCatching {
                context.assets.open(photo.assetPath).use { BitmapFactory.decodeStream(it) }
            }.getOrNull()
        }
    }.value ?: return

    val described = stringResource(R.string.photo_of, speciesName)
    Column(modifier.fillMaxWidth()) {
        Image(
            bitmap = image.asImageBitmap(),
            contentDescription = described,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(3f / 2f)
                .clip(RoundedCornerShape(Radius.xlarge))
                .border(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f),
                    shape = RoundedCornerShape(Radius.xlarge),
                ),
        )
        PhotoCredit(photo)
    }
}

/** "Foto: Navn · Licens · Wikimedia Commons", linked to the Commons page. */
@Composable
private fun PhotoCredit(photo: IdentifiedPhoto) {
    val uriHandler = LocalUriHandler.current
    val credit = stringResource(R.string.photo_credit, photo.photo.artist, photo.photo.license)
    val open = stringResource(R.string.photo_open_commons)
    Text(
        credit,
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        maxLines = 2,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier
            .minimumInteractiveComponentSize()
            .clickable {
                runCatching { uriHandler.openUri(photo.photo.sourceURL) }
            }
            .padding(top = Spacing.xs, bottom = Spacing.xxs, start = Spacing.xxs)
            .semantics {
                contentDescription = "$credit. $open"
            },
    )
}
