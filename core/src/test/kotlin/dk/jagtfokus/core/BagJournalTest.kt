package dk.jagtfokus.core

import java.time.Instant
import java.time.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class BagJournalTest {
    private val calendar = danishCalendar()
    private val now = Instant.parse("2026-10-19T10:00:00Z")

    private fun journal(vararg entries: BagEntry) = BagJournal(entries.toList())

    private fun entry(
        id: String,
        date: String,
        count: Int = 1,
        speciesId: String = "raev",
        createdAt: String = "2026-10-19T10:00:00Z",
        placeId: String? = null,
    ) = BagEntry(
        id = id,
        createdAt = createdAt,
        date = date,
        speciesId = speciesId,
        count = count,
        placeId = placeId,
    )

    @Test
    fun recordAddsOnePiece() {
        val bag = BagJournal().record("raev", day(2026, 10, 19), "als", now)
        assertEquals(1, bag.entries.size)
        assertEquals(1, bag.entries[0].count)
        assertEquals("2026-10-19", bag.entries[0].date)
        assertEquals("raev", bag.entries[0].speciesId)
        assertEquals("als", bag.entries[0].placeId)
    }

    @Test
    fun incrementUpdatesTheSameEntry() {
        var bag = BagJournal().record("raev", day(2026, 10, 19), null, now)
        val id = bag.entries[0].id
        bag = bag.increment(id, now)
        assertEquals(1, bag.entries.size)
        assertEquals(2, bag.entries[0].count)
    }

    @Test
    fun removeDeletesTheEntry() {
        var bag = BagJournal().record("raev", day(2026, 10, 19), null, now)
        val id = bag.entries[0].id
        bag = bag.remove(id)
        assertTrue(bag.entries.isEmpty())
        assertNull(bag.entry(id))
    }

    @Test
    fun updateChangesFieldsAndKeepsCountAtLeastOne() {
        var bag = BagJournal().record("raev", day(2026, 10, 19), null, now)
        val id = bag.entries[0].id
        bag = bag.update(id, "2026-10-20", count = 3, placeId = "als", now = now)
        assertEquals("2026-10-20", bag.entries[0].date)
        assertEquals(3, bag.entries[0].count)
        assertEquals("als", bag.entries[0].placeId)
        bag = bag.update(id, "2026-10-20", count = 0, placeId = null, now = now)
        assertEquals(1, bag.entries[0].count)
    }

    @Test
    fun summaryCountsPiecesAndSpeciesInOneHuntingYear() {
        val bag = journal(
            entry("1", "2026-10-19", count = 1, speciesId = "raev"),
            entry("2", "2026-10-20", count = 2, speciesId = "raev"),
            entry("3", "2026-10-20", count = 1, speciesId = "hare"),
            entry("4", "2026-03-20", count = 5, speciesId = "raev"),
            entry("5", "2025-10-20", count = 7, speciesId = "hare"),
        )
        // Hunting year 2026/27 runs 1 April 2026 – 31 March 2027.
        assertEquals(4, bag.summary(2026).pieces)
        assertEquals(2, bag.summary(2026).speciesCount)
        // Hunting year 2025/26 holds both March 2026 and October 2025.
        assertEquals(12, bag.summary(2025).pieces)
    }

    @Test
    fun rowsSortByNewestDayThenNewestNote() {
        val bag = journal(
            entry("1", "2026-10-19", createdAt = "2026-10-19T08:00:00Z"),
            entry("2", "2026-10-20", createdAt = "2026-10-20T08:00:00Z"),
            entry("3", "2026-10-19", createdAt = "2026-10-19T12:00:00Z"),
        )
        assertEquals(listOf("2", "3", "1"), bag.entries(2026).map { it.id })
    }

    @Test
    fun recentSpeciesIdsAreNewestFirstOneEach() {
        val bag = journal(
            entry("1", "2026-10-19", speciesId = "raev", createdAt = "2026-10-19T08:00:00Z"),
            entry("2", "2026-10-19", speciesId = "hare", createdAt = "2026-10-19T09:00:00Z"),
            entry("3", "2026-10-19", speciesId = "raev", createdAt = "2026-10-19T10:00:00Z"),
        )
        assertEquals(listOf("raev", "hare"), bag.recentSpeciesIds())
    }

    @Test
    fun huntingYearFollowsApril() {
        assertEquals(2025, calendar.huntingYear(LocalDate.of(2026, 3, 20)))
        assertEquals(2026, calendar.huntingYear(LocalDate.of(2026, 4, 1)))
        assertEquals(2026, calendar.huntingYear(LocalDate.of(2026, 10, 19)))
        assertEquals("2026/27", calendar.huntingYearLabel(2026))
    }

    @Test
    fun encodeAndDecodeRoundTrips() {
        val bag = BagJournal().record("raev", day(2026, 10, 19), "als", now)
        val decoded = BagJournal.decode(BagJournal.encode(bag))
        assertEquals(bag, decoded)
    }

    @Test
    fun checklistSeedsFromJournalOnceAndSurvivesDeletions() {
        var bag = BagJournal().record("raev", day(2026, 10, 19), null, now)
        var list = SpeciesChecklist().seededFrom(bag)
        assertTrue(list.contains("raev"))
        // A later journal deletion must not empty the saved list.
        bag = bag.remove(bag.entries[0].id)
        list = list.remove("raev")
        assertTrue(!list.contains("raev"))
    }
}
