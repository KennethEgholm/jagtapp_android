package dk.jagtfokus.app.data

import android.content.Context
import dk.jagtfokus.core.BiologyCatalog
import dk.jagtfokus.core.HuntingCatalog
import dk.jagtfokus.core.SpeciesPhotos
import dk.jagtfokus.core.decodeChecked

/** The bundled file is a copy of the iOS repo's data. Remote update comes later. */
fun loadCatalog(context: Context): HuntingCatalog =
    context.assets.open("jagttider.json").use { decodeChecked(it.readBytes()) }

/** Display only. A missing file just means no biology section. */
fun loadBiology(context: Context): BiologyCatalog =
    context.assets.open("biologi.json").use { BiologyCatalog.decode(it.readBytes()) }

/** Photo credits. A missing file or image means no photo card. */
fun loadSpeciesPhotos(context: Context): SpeciesPhotos =
    context.assets.open("species-photos.json").use { SpeciesPhotos.decode(it.readBytes()) }
