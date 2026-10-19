package dk.jagtfokus.core

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Photo credits from `data/species-photos.json`.
 * Image files live in the app assets as `photos/photo-<id>.jpg`.
 */
@Serializable
data class SpeciesPhotos(
    /** Keyed by photo id. */
    val photos: Map<String, SpeciesPhoto>,
    /** Species id → photo id. Several species may share one photo. */
    val species: Map<String, String> = emptyMap(),
) {
    fun photo(forSpeciesId: String): IdentifiedPhoto? {
        val id = species[forSpeciesId] ?: return null
        return photos[id]?.let { IdentifiedPhoto(id, it) }
    }

    companion object {
        fun decode(bytes: ByteArray): SpeciesPhotos =
            catalogJson.decodeFromString(bytes.decodeToString())
    }
}

@Serializable
data class IdentifiedPhoto(
    val id: String,
    val photo: SpeciesPhoto,
) {
    /** Asset path in the app's assets. */
    val assetPath: String
        get() = "photos/photo-$id.jpg"
}

@Serializable
data class SpeciesPhoto(
    /** Wikimedia Commons file title. */
    val file: String,
    val artist: String,
    val license: String,
    @SerialName("license_url") val licenseURL: String? = null,
    /** Commons file page. Shows full license and author. */
    @SerialName("source_url") val sourceURL: String,
) {
    /** CC0, public domain, CC BY and CC BY-SA. Never NC or ND. */
    val allowsCommercialUse: Boolean
        get() {
            val value = license.lowercase()
            if (value.contains("-nc") || value.contains("-nd")) return false
            return value == "cc0" ||
                value.startsWith("public domain") ||
                value.startsWith("cc by ") ||
                value.startsWith("cc by-sa ")
        }
}
