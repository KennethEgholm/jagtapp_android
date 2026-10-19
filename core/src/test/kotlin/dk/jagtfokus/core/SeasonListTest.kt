package dk.jagtfokus.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SeasonListTest {
    private val calendar = danishCalendar()

    private fun species(id: String, name: String, periods: List<Pair<String, String>>): ResolvedSpecies {
        val parsed = periods.map { period(it.first, it.second) }
        return ResolvedSpecies(Species(id, name, parsed, null), parsed, null)
    }

    private val groups = listOf(
        ResolvedGroup(
            "pattedyr",
            "Pattedyr",
            listOf(
                species("raev", "Ræv", listOf("09-01" to "01-31")),
                species("hare", "Hare", listOf("10-01" to "12-15")),
                species("ilder", "Ilder", emptyList()),
            ),
        ),
        ResolvedGroup(
            "fugle",
            "Fugle",
            listOf(
                species("gaas", "Grågås", listOf("09-01" to "10-20")),
                species("and", "And", listOf("11-01" to "12-31")),
                species("bramgaas", "Bramgås", listOf("11-01" to "12-31")),
            ),
        ),
        ResolvedGroup(
            "lukket",
            "Lukket",
            listOf(species("sneppe", "Sneppe", listOf("10-01" to "10-10"))),
        ),
    )

    @Test
    fun openGroupsKeepOnlyOpenSpeciesAndDropEmptyGroups() {
        val open = calendar.openGroups(groups, day(2026, 10, 17))
        assertEquals(listOf("pattedyr", "fugle"), open.map { it.id })
        assertEquals(listOf("raev", "hare"), open[0].species.map { it.id })
        assertEquals(listOf("gaas"), open[1].species.map { it.id })
    }

    @Test
    fun closedSpeciesSortBySoonestOpeningThenName() {
        val closed = calendar.closedSpecies(groups, day(2026, 10, 17))
        assertEquals(listOf("and", "bramgaas", "sneppe", "ilder"), closed.map { it.id })
    }

    @Test
    fun closingSoonCountsOpenSpeciesWithLastDayWithinAWeek() {
        assertEquals(1, calendar.closingSoonCount(groups, day(2026, 10, 17)))
        assertEquals(0, calendar.closingSoonCount(groups, day(2026, 10, 1)))
    }
}

class TextSearchTest {
    @Test
    fun ignoresCaseAndAccents() {
        assertTrue(TextSearch.matches("Ræv", "RÆV"))
        assertTrue(TextSearch.matches("Grågås", "gragas"))
        assertTrue(TextSearch.matches("Dåhjort", "då"))
    }

    @Test
    fun trimsAndTreatsEmptyAsEverything() {
        assertTrue(TextSearch.matches("Råbuk", "  råb "))
        assertTrue(TextSearch.matches("Råbuk", "   "))
    }

    @Test
    fun missesWhatIsNotThere() {
        assertFalse(TextSearch.matches("Råbuk", "hjort"))
    }

    @Test
    fun anyNameCanMatch() {
        assertTrue(TextSearch.matches(listOf("Als", "Sønderborg"), "SØNDER"))
        assertFalse(TextSearch.matches(listOf("Als", "Sønderborg"), "Ærø"))
    }
}
