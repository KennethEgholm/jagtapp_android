package dk.jagtfokus.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class PlaceResolutionTest {
    private val calendar = danishCalendar()
    private val catalog = bundledCatalog()

    private fun species(id: String) = catalog.allSpecies.first { it.id == id }
    private fun place(id: String) = catalog.place(id)!!

    @Test
    fun catalogHasPlacesAndOverrides() {
        assertEquals(6, catalog.version)
        assertTrue(catalog.places.size >= 10)
        assertTrue(catalog.overrides.isNotEmpty())
    }

    @Test
    fun gpsAarhusMapsToMidtjylland() {
        assertEquals("midtjylland", catalog.matchPlace(listOf("Aarhus", "Region Midtjylland")).id)
    }

    @Test
    fun gpsEndelaveBeatsMidtjylland() {
        assertEquals("endelave", catalog.matchPlace(listOf("Endelave", "Horsens", "Region Midtjylland")).id)
    }

    @Test
    fun townsContainingIslandNamesDoNotMatchIsland() {
        assertEquals("nordjylland", catalog.matchPlace(listOf("Hals", "Aalborg Kommune", "Region Nordjylland")).id)
        assertEquals("lolland_falster", catalog.matchPlace(listOf("Nørre Alslev", "Guldborgsund Kommune", "Region Sjælland")).id)
        assertEquals("midtjylland", catalog.matchPlace(listOf("Ry", "Skanderborg Kommune", "Region Midtjylland")).id)
    }

    @Test
    fun streetNamesDoNotMatchIsland() {
        assertEquals("hovedstaden_kyst", catalog.matchPlace(listOf("Marstalsgade 1", "København", "Region Hovedstaden")).id)
    }

    @Test
    fun kommuneSuffixIsIgnored() {
        assertEquals("syddanmark_vest", catalog.matchPlace(listOf("Tønder Kommune", "Region Syddanmark")).id)
        assertEquals("als", catalog.matchPlace(listOf("Nordborg", "Sønderborg Kommune", "Region Syddanmark")).id)
    }

    @Test
    fun hareClosedOnEndelaveWhenOpenInMidtjylland() {
        val date = day(2026, 11, 10)
        val midt = catalog.resolved(species("hare"), place("midtjylland"))
        val island = catalog.resolved(species("hare"), place("endelave"))
        assertTrue(calendar.isOpen(midt.periods, date))
        assertFalse(calendar.isOpen(island.periods, date))
    }

    @Test
    fun saedgaasOnlyOpenOnLolland() {
        val date = day(2026, 9, 15)
        val dk = catalog.resolved(species("saedgaas"), place("dk"))
        val lolland = catalog.resolved(species("saedgaas"), place("lolland_falster"))
        assertFalse(calendar.isOpen(dk.periods, date))
        assertTrue(calendar.isOpen(lolland.periods, date))
    }

    @Test
    fun resolvedSpeciesNamesItsLocalPlace() {
        val als = catalog.resolved(species("raabuk"), place("als"))
        val dk = catalog.resolved(species("raabuk"), place("dk"))
        assertEquals("als", als.localPlaceId)
        assertTrue(als.isLocal)
        assertNull(dk.localPlaceId)
        assertFalse(dk.isLocal)
    }

    @Test
    fun regionOverrideAppliesOnIslandBelowIt() {
        assertEquals("syddanmark", catalog.resolved(species("agerhoene"), place("als")).localPlaceId)
    }

    @Test
    fun resolvedSectionsKeepAllGroupsAndSpecies() {
        val sections = catalog.resolvedSections(place("sejero"))
        assertEquals(catalog.groups.map { it.id }, sections.map { it.id })
        assertEquals(catalog.allSpecies.size, sections.flatMap { it.species }.size)
        val hare = sections.flatMap { it.species }.first { it.id == "hare" }
        assertTrue(hare.isLocal)
        assertEquals(species("hare").periods, hare.generalPeriods)
    }

    @Test
    fun localTimeCountPerPlace() {
        assertEquals(5, catalog.localTimeCount("als"))
        assertEquals(0, catalog.localTimeCount("dk"))
    }

    @Test
    fun localTimeDoesNotRepeatGeneralNote() {
        val hare = catalog.resolved(species("hare"), place("midtjylland"))
        assertTrue(hare.isLocal)
        assertNull(hare.note)
    }

    @Test
    fun localTimeKeepsItsOwnNote() {
        val fasan = catalog.resolved(species("fasan"), place("sejero"))
        assertEquals("Hane 1. nov–31. jan. Høne kun 16. nov–30. nov.", fasan.note)
    }
}
