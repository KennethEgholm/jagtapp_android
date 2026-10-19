package dk.jagtfokus.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** Ports SpeciesPhotoTests.swift. Reads the bundled data and the iOS repo's photo credits. */
class SpeciesPhotoTest {
    private val catalog = bundledCatalog()
    private val photos = SpeciesPhotos.decode(repoFile("data/species-photos.json").readBytes())

    @Test
    fun everySpeciesHasAPhoto() {
        val missing = catalog.allSpecies.map { it.id }.filter { photos.photo(it) == null }
        assertTrue(missing.isEmpty(), "Mangler foto: $missing")
    }

    @Test
    fun sharedPhotoResolvesToItsOwner() {
        assertEquals("graagaas", photos.photo("graagaas_omdrift")?.id)
        assertEquals("graagaas", photos.photo("graagaas")?.id)
    }

    @Test
    fun licensesAllowCommercialUse() {
        for (photo in photos.photos.values) {
            assertTrue(photo.allowsCommercialUse, "${photo.file}: ${photo.license}")
        }
    }

    @Test
    fun licensesMatchCommercialRule() {
        fun photo(license: String) = SpeciesPhoto(
            file = "f", artist = "a", license = license, licenseURL = null, sourceURL = "s",
        )
        assertTrue(photo("CC BY-SA 4.0").allowsCommercialUse)
        assertTrue(photo("CC BY 2.0").allowsCommercialUse)
        assertTrue(photo("CC0").allowsCommercialUse)
        assertTrue(photo("Public domain").allowsCommercialUse)
        assertFalse(photo("CC BY-NC 4.0").allowsCommercialUse)
        assertFalse(photo("CC BY-ND 4.0").allowsCommercialUse)
        assertFalse(photo("GFDL").allowsCommercialUse)
        assertFalse(photo("CC BY-NC-SA 2.0").allowsCommercialUse)
        assertTrue(photo("CC BY-SA 3.0 at").allowsCommercialUse)
    }

    @Test
    fun everyPhotoIsCredited() {
        for (photo in photos.photos.values) {
            assertTrue(photo.artist.isNotEmpty(), photo.file)
            assertTrue(photo.sourceURL.startsWith("https://commons.wikimedia.org/"), photo.file)
        }
    }

    @Test
    fun everyPhotoHasAnImageAsset() {
        for (id in photos.photos.keys) {
            assertTrue(repoFile("app/src/main/assets/photos/photo-$id.jpg").isFile, "Mangler billede: $id")
        }
    }

    @Test
    fun noUnusedPhotos() {
        val used = photos.species.values.toSet()
        assertEquals(used, photos.photos.keys)
    }
}
