package dk.jagtfokus.core

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.text.Normalizer
import java.util.Locale

@Serializable
enum class PlaceKind {
    @SerialName("country") COUNTRY,
    @SerialName("region") REGION,
    @SerialName("area") AREA,
    @SerialName("island") ISLAND,
}

@Serializable
data class Place(
    val id: String,
    val name: String,
    val kind: PlaceKind,
    @SerialName("parent_id") val parentId: String? = null,
    @SerialName("match_names") val matchNames: List<String> = emptyList(),
    /** Representative point, for sun times. Optional so older data still decodes. */
    @SerialName("lat") val latitude: Double? = null,
    @SerialName("lon") val longitude: Double? = null,
)

@Serializable
data class LocalOverride(
    @SerialName("species_id") val speciesId: String,
    @SerialName("place_id") val placeId: String,
    val periods: List<SeasonPeriod>,
    val note: String? = null,
)

data class ResolvedSpecies(
    val species: Species,
    val periods: List<SeasonPeriod>,
    val note: String?,
    /** Place whose local override applies, or null for the general time. */
    val localPlaceId: String? = null,
) {
    val id: String get() = species.id
    val name: String get() = species.name
    val isLocal: Boolean get() = localPlaceId != null
    val generalPeriods: List<SeasonPeriod> get() = species.periods
}

data class ResolvedGroup(
    val id: String,
    val title: String,
    val species: List<ResolvedSpecies>,
)

val HuntingCatalog.denmark: Place
    get() = places.firstOrNull { it.id == "dk" }
        ?: Place(id = "dk", name = "Danmark", kind = PlaceKind.COUNTRY, matchNames = listOf("Danmark"))

fun HuntingCatalog.place(id: String): Place? = places.firstOrNull { it.id == id }

/** The place's own point, else the nearest ancestor's. */
fun HuntingCatalog.coordinate(place: Place): Pair<Double, Double>? {
    for (node in ancestry(place)) {
        val latitude = node.latitude
        val longitude = node.longitude
        if (latitude != null && longitude != null) return latitude to longitude
    }
    return null
}

fun HuntingCatalog.ancestry(place: Place): List<Place> {
    val chain = mutableListOf<Place>()
    val seen = mutableSetOf<String>()
    var current: Place? = place
    while (current != null && seen.add(current.id)) {
        chain += current
        current = current.parentId?.let { place(it) }
    }
    return chain
}

/**
 * Most specific place whose name equals one of the geocoder's names.
 * Whole names only: "Hals" must not hit "Als", nor "Ry" hit "Strynø".
 */
fun HuntingCatalog.matchPlace(names: List<String>): Place {
    val needles = names.map { matchKey(it) }.filter { it.isNotEmpty() }.toSet()
    if (needles.isEmpty()) return denmark
    var best: Place? = null
    var bestScore = 0
    for (place in places) {
        val keys = (listOf(place.name) + place.matchNames).map { matchKey(it) }
        if (keys.none { it in needles }) continue
        val score = specificity(place.kind)
        if (best == null || score > bestScore) {
            best = place
            bestScore = score
        }
    }
    return best ?: denmark
}

fun HuntingCatalog.resolved(species: Species, at: Place): ResolvedSpecies {
    for (placeId in ancestry(at).map { it.id }) {
        val override = overrides.firstOrNull { it.speciesId == species.id && it.placeId == placeId }
        if (override != null) {
            return ResolvedSpecies(
                species = species,
                periods = override.periods,
                // The general note describes the general time. A local time carries its own.
                note = override.note,
                localPlaceId = placeId,
            )
        }
    }
    return ResolvedSpecies(species = species, periods = species.periods, note = species.note)
}

fun HuntingCatalog.resolvedSections(at: Place): List<ResolvedGroup> =
    groups.map { group ->
        ResolvedGroup(
            id = group.id,
            title = group.title,
            species = group.species.map { resolved(it, at) },
        )
    }

fun HuntingCatalog.localTimeCount(placeId: String): Int =
    overrides.count { it.placeId == placeId }

private fun specificity(kind: PlaceKind): Int = when (kind) {
    PlaceKind.ISLAND -> 4
    PlaceKind.AREA -> 3
    PlaceKind.REGION -> 2
    PlaceKind.COUNTRY -> 1
}

/** Folded, trimmed, without a trailing " Kommune". */
internal fun matchKey(name: String): String {
    var key = foldDanish(name).trim()
    if (key.endsWith(" kommune")) {
        key = key.dropLast(" kommune".length)
    }
    return key
}

internal fun foldDanish(text: String): String {
    val lower = text.lowercase(Locale.forLanguageTag("da-DK"))
    val mapped = buildString(lower.length) {
        for (ch in lower) {
            when (ch) {
                'æ' -> append("ae")
                'ø' -> append("o")
                'å' -> append("a")
                else -> append(ch)
            }
        }
    }
    return Normalizer.normalize(mapped, Normalizer.Form.NFD)
        .replace("\\p{M}+".toRegex(), "")
}
