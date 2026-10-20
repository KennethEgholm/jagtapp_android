package dk.jagtfokus.app.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import dk.jagtfokus.app.R

/** The JagtFokus mark with the app icon's corner ratio. Decorative: the name is always beside it. */
@Composable
fun BrandMark(size: Dp, modifier: Modifier = Modifier) {
    Image(
        painter = painterResource(R.drawable.brand_mark),
        contentDescription = null,
        modifier = modifier.size(size).clip(RoundedCornerShape(size * 0.225f)),
    )
}
