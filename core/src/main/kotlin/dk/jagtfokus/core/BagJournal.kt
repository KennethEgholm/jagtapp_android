package dk.jagtfokus.core

import java.time.Instant
import java.time.LocalDate
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * One journal note: one species, one calendar day, one count. Day keys are
 * `yyyy-MM-dd` in Europe/Copenhagen. Display only; the app is not the law.
 */
@Serializable
data class BagEntry(
    val id: String,
    @SerialName("created_at") val createdAt: String,
    @SerialName("updated_at") val updatedAt: String? = null,
    val date: String,
    @SerialName("species_id") val speciesId: String,
    val count: Int,
    @SerialName("place_id") val placeId: String? = null,
)

data class BagYearSummary(
    val pieces: Int,
    val speciesCount: Int,
)

/** Hunting year containing the date. 1 April starts the year named by that calendar year. */
fun huntingYearOf(on: LocalDate): Int =
    if (on.month.value >= SeasonTimeline.START_MONTH) on.year else on.year - 1

/**
 * The local bag. No cloud sync on Android: the file stays on the phone.
 * Immutably mutated, so Compose state and tests get a new value per change.
 */
@Serializable
data class BagJournal(
    val entries: List<BagEntry> = emptyList(),
) {
    /** One piece. Date is the calendar day of `on`, not a browsed query date. */
    fun record(speciesId: String, date: LocalDate, placeId: String?, now: Instant): BagJournal {
        val entry = BagEntry(
            id = java.util.UUID.randomUUID().toString(),
            createdAt = now.toString(),
            date = date.toString(),
            speciesId = speciesId,
            count = 1,
            placeId = placeId,
        )
        return copy(entries = entries + entry)
    }

    /** Adds one to the entry. Does not create a new one. */
    fun increment(id: String, now: Instant): BagJournal = copy(
        entries = entries.map { entry ->
            if (entry.id == id) entry.copy(count = entry.count + 1, updatedAt = now.toString()) else entry
        },
    )

    fun remove(id: String): BagJournal = copy(entries = entries.filterNot { it.id == id })

    /** Puts a deleted entry back, as it was. An entry already present is left alone. */
    fun restore(entry: BagEntry): BagJournal =
        if (entries.any { it.id == entry.id }) this else copy(entries = entries + entry)

    fun update(id: String, date: String, count: Int, placeId: String?, now: Instant): BagJournal = copy(
        entries = entries.map { entry ->
            if (entry.id == id) {
                entry.copy(date = date, count = maxOf(1, count), placeId = placeId, updatedAt = now.toString())
            } else {
                entry
            }
        },
    )

    fun entry(id: String): BagEntry? = entries.firstOrNull { it.id == id }

    /** This hunting year's rows, newest day first, newest note first within a day. */
    fun entries(inHuntingYear: Int): List<BagEntry> =
        entries
            .filter { entry ->
                runCatching { LocalDate.parse(entry.date) }
                    .map { huntingYearOf(it) == inHuntingYear }
                    .getOrDefault(false)
            }
            .sortedWith(compareByDescending<BagEntry> { it.date }.thenByDescending { it.createdAt })

    fun summary(inHuntingYear: Int): BagYearSummary {
        val rows = entries(inHuntingYear)
        return BagYearSummary(
            pieces = rows.sumOf { it.count },
            speciesCount = rows.map { it.speciesId }.toSet().size,
        )
    }

    /** Newest first, one id per species. */
    fun recentSpeciesIds(limit: Int = 8): List<String> {
        val seen = mutableSetOf<String>()
        return entries
            .sortedByDescending { it.createdAt }
            .map { it.speciesId }
            .filter { seen.add(it) }
            .take(limit)
    }

    companion object {
        fun decode(bytes: ByteArray): BagJournal =
            catalogJson.decodeFromString(bytes.decodeToString())

        fun encode(journal: BagJournal): ByteArray =
            catalogJson.encodeToString(journal).encodeToByteArray()
    }
}
