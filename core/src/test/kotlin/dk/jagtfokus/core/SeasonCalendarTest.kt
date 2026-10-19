package dk.jagtfokus.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SeasonCalendarTest {
    private val calendar = danishCalendar()
    private val catalog = bundledCatalog()

    private fun species(id: String) = catalog.allSpecies.first { it.id == id }

    @Test
    fun catalogHasGroupedSpecies() {
        assertEquals(7, catalog.groups.size)
        assertTrue(catalog.allSpecies.size >= 40)
        assertEquals(6, catalog.version)
    }

    @Test
    fun octoberTenthOpensRoeAndClosesHare() {
        val date = day(2026, 10, 10)
        assertTrue(calendar.isOpen(species("raabuk").periods, date))
        assertTrue(calendar.isOpen(species("raa_og_lam").periods, date))
        assertTrue(calendar.isOpen(species("fasan").periods, date))
        assertFalse(calendar.isOpen(species("hare").periods, date))
        assertFalse(calendar.isOpen(species("ringdue").periods, date))
        assertFalse(calendar.isOpen(species("kronhjort").periods, date))
    }

    @Test
    fun yearWrapIncludesJanuary() {
        val date = day(2027, 1, 10)
        assertTrue(calendar.isOpen(species("raabuk").periods, date))
        assertTrue(calendar.isOpen(species("raev").periods, date))
        assertFalse(calendar.isOpen(species("graaand").periods, date))
    }

    @Test
    fun buckHasTwoPeriods() {
        assertTrue(calendar.isOpen(species("raabuk").periods, day(2026, 5, 16)))
        assertTrue(calendar.isOpen(species("raabuk").periods, day(2026, 7, 15)))
        assertFalse(calendar.isOpen(species("raabuk").periods, day(2026, 7, 16)))
        assertFalse(calendar.isOpen(species("raabuk").periods, day(2026, 9, 30)))
        assertTrue(calendar.isOpen(species("raabuk").periods, day(2026, 10, 1)))
    }

    @Test
    fun augustGooseIsSeparateEntry() {
        assertTrue(calendar.isOpen(species("graagaas_omdrift").periods, day(2026, 8, 15)))
        assertFalse(calendar.isOpen(species("graagaas").periods, day(2026, 8, 15)))
        assertTrue(calendar.isOpen(species("graagaas").periods, day(2026, 9, 1)))
    }

    @Test
    fun nextOpeningForClosedHare() {
        assertEquals(day(2026, 11, 1), calendar.nextOpening(species("hare").periods, day(2026, 10, 10)))
    }

    @Test
    fun nextOpeningIsNilWhenOpen() {
        assertNull(calendar.nextOpening(species("fasan").periods, day(2026, 10, 10)))
    }

    @Test
    fun kronkalvIncludesFeb28InNonLeapYear() {
        assertTrue(calendar.isOpen(species("kronkalv").periods, day(2027, 2, 28)))
        assertFalse(calendar.isOpen(species("kronkalv").periods, day(2027, 3, 1)))
    }

    @Test
    fun periodTextJoinsTwoSeasons() {
        assertEquals(
            "16. maj – 15. jul. og 1. okt. – 31. jan.",
            calendar.periodText(species("raabuk").periods),
        )
    }

    @Test
    fun redAndFallowDeerAreExactlyTheKronAndDaSpecies() {
        val byName = catalog.allSpecies.filter { it.name.startsWith("Kron") || it.name.startsWith("Då") }
        val flagged = catalog.allSpecies.filter { it.isRedOrFallowDeer }
        assertEquals(byName.map { it.id }.toSet(), flagged.map { it.id }.toSet())
        assertEquals(6, flagged.size)
    }
}
