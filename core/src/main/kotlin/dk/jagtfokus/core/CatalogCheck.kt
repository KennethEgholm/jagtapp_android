package dk.jagtfokus.core

import java.net.URI

/** Why a downloaded catalog must not replace the one the app has. */
sealed class CatalogProblem {
    data object NoSpecies : CatalogProblem()
    data object NoDenmark : CatalogProblem()
    data class RepeatedSpecies(val id: String) : CatalogProblem()
    data class RepeatedPlace(val id: String) : CatalogProblem()
    data class UnknownParent(val place: String, val parent: String) : CatalogProblem()
    data class OverrideForUnknownSpecies(val id: String) : CatalogProblem()
    data class OverrideForUnknownPlace(val id: String) : CatalogProblem()
    data class UntrustedSource(val source: String) : CatalogProblem()
}

class CatalogRefused(val problems: List<CatalogProblem>) : Exception("Catalog refused: $problems")

/** The law lives on Retsinformation. The source link may point nowhere else. */
const val SOURCE_HOST = "retsinformation.dk"

/** Empty means the file is fit to show. A file that decodes can still be broken. */
val HuntingCatalog.problems: List<CatalogProblem>
    get() {
        val found = mutableListOf<CatalogProblem>()
        val species = allSpecies.map { it.id }
        val placeIds = places.map { it.id }.toSet()
        if (species.isEmpty()) found += CatalogProblem.NoSpecies
        if ("dk" !in placeIds) found += CatalogProblem.NoDenmark
        found += repeated(species).map { CatalogProblem.RepeatedSpecies(it) }
        found += repeated(places.map { it.id }).map { CatalogProblem.RepeatedPlace(it) }
        for (place in places) {
            val parent = place.parentId
            if (parent != null && parent !in placeIds) {
                found += CatalogProblem.UnknownParent(place.id, parent)
            }
        }
        val speciesIds = species.toSet()
        for (override in overrides) {
            if (override.speciesId !in speciesIds) {
                found += CatalogProblem.OverrideForUnknownSpecies(override.speciesId)
            }
            if (override.placeId !in placeIds) {
                found += CatalogProblem.OverrideForUnknownPlace(override.placeId)
            }
        }
        if (sourceLink == null) found += CatalogProblem.UntrustedSource(sourceURL)
        return found
    }

/** The source as a link, only over https to Retsinformation. The original string, if it is trusted. */
val HuntingCatalog.sourceLink: String?
    get() {
        val uri = runCatching { URI(sourceURL) }.getOrNull() ?: return null
        if (!uri.scheme.equals("https", ignoreCase = true)) return null
        if (uri.userInfo != null) return null
        val host = uri.host?.lowercase() ?: return null
        if (host != SOURCE_HOST && host != "www.$SOURCE_HOST") return null
        return sourceURL
    }

/** For downloaded and cached files. Throws [CatalogRefused], so the app keeps the data it has. */
fun decodeChecked(bytes: ByteArray): HuntingCatalog {
    val catalog = HuntingCatalog.decode(bytes)
    val problems = catalog.problems
    if (problems.isNotEmpty()) throw CatalogRefused(problems)
    return catalog
}

private fun repeated(ids: List<String>): List<String> {
    val seen = mutableSetOf<String>()
    val repeated = mutableListOf<String>()
    for (id in ids) {
        if (!seen.add(id) && id !in repeated) repeated += id
    }
    return repeated
}
