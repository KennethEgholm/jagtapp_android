package dk.jagtfokus.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import dk.jagtfokus.app.data.loadBiology
import dk.jagtfokus.app.data.loadCatalog
import dk.jagtfokus.app.data.loadSpeciesPhotos
import dk.jagtfokus.app.jagtbart.JagtbartScreen
import dk.jagtfokus.app.ui.theme.JagtFokusTheme
import dk.jagtfokus.app.ui.theme.Spacing
import java.time.LocalDate
import java.time.format.DateTimeParseException

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        val catalog = runCatching { loadCatalog(this) }.getOrNull()
        val biology = runCatching { loadBiology(this) }.getOrNull()
        val photos = runCatching { loadSpeciesPhotos(this) }.getOrNull()
        val date = intent.getStringExtra("date")?.let {
            try {
                LocalDate.parse(it)
            } catch (_: DateTimeParseException) {
                null
            }
        }
        val placeId = intent.getStringExtra("place")
        setContent {
            JagtFokusTheme {
                if (catalog == null) {
                    Box(Modifier.fillMaxSize().padding(Spacing.l), contentAlignment = Alignment.Center) {
                        Text(
                            stringResource(R.string.catalog_error),
                            style = MaterialTheme.typography.bodyLarge,
                        )
                    }
                } else {
                    JagtbartScreen(catalog, biology, photos, date, placeId)
                }
            }
        }
    }
}
