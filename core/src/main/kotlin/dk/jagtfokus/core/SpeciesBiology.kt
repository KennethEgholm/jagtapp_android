package dk.jagtfokus.core

import kotlinx.serialization.Serializable

/**
 * Biological notes from `data/biologi.json`.
 * Display only. Never used to decide whether a species is open.
 *
 * Birds: DOF, Danmarks Fugle. Mammals: Arter.dk texts from Miljøstyrelsen,
 * with Lex or Dansk Pattedyratlas where Arter has no life-history text.
 * "Invasiv" only when Styrelsen for Grøn Arealomlægning og Vandmiljø lists
 * the species for year-round regulation. No population estimates.
 */
@Serializable
data class BiologyCatalog(
    val version: Int,
    val updated: String,
    val taxa: Map<String, TaxonBiology>,
    /** Hunting species id → taxon id. Sex and age classes share one taxon. */
    val species: Map<String, String> = emptyMap(),
) {
    fun biology(forSpeciesId: String): TaxonBiology? {
        val taxonId = species[forSpeciesId] ?: return null
        return taxa[taxonId]
    }

    companion object {
        fun decode(bytes: ByteArray): BiologyCatalog =
            catalogJson.decodeFromString(bytes.decodeToString())
    }
}

@Serializable
data class TaxonBiology(
    val latin: String,
    val facts: List<BiologyFact> = emptyList(),
    val sources: List<BiologySource> = emptyList(),
)

@Serializable
data class BiologyFact(
    val label: String,
    val value: String,
)

@Serializable
data class BiologySource(
    val name: String,
    val url: String,
)
