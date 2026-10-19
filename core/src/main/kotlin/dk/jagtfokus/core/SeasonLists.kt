package dk.jagtfokus.core

import java.time.LocalDate

/** Each group with only the species that are open on the day. Groups with none drop out. */
fun SeasonCalendar.openGroups(groups: List<ResolvedGroup>, on: LocalDate): List<ResolvedGroup> =
    groups.mapNotNull { group ->
        val open = group.species.filter { status(it.periods, on).isOpen }
        if (open.isEmpty()) null else ResolvedGroup(group.id, group.title, open)
    }

/** Species closed on the day, soonest opening first. No season at all goes last. Ties by name. */
fun SeasonCalendar.closedSpecies(groups: List<ResolvedGroup>, on: LocalDate): List<ResolvedSpecies> =
    groups.flatMap { it.species }
        .mapNotNull { item ->
            val state = status(item.periods, on)
            if (state.isOpen) null else item to (state.daysUntilOpening ?: Int.MAX_VALUE)
        }
        .sortedWith(compareBy({ it.second }, { it.first.name }))
        .map { it.first }

/** Open species whose last day is within a week. */
fun SeasonCalendar.closingSoonCount(groups: List<ResolvedGroup>, on: LocalDate): Int =
    groups.flatMap { it.species }.count { status(it.periods, on).isClosingSoon }

/** Search in species and place names. The names are Danish in both app languages. */
object TextSearch {
    /** Ignores case and accents, so "gragas" finds "Grågås". An empty query matches everything. */
    fun matches(text: String, query: String): Boolean {
        val needle = foldDanish(query.trim())
        return needle.isEmpty() || foldDanish(text).contains(needle)
    }

    /** One of several names, e.g. a place and its other names. */
    fun matches(texts: List<String>, query: String): Boolean =
        texts.any { matches(it, query) }
}
