package dk.jagtfokus.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue

class CatalogCheckTest {
    private val law = "https://www.retsinformation.dk/eli/lta/2024/470"

    private fun catalog(
        species: List<Species> = listOf(Species("raev", "Ræv", emptyList(), null)),
        places: List<Place> = listOf(
            Place("dk", "Danmark", PlaceKind.COUNTRY, matchNames = emptyList()),
            Place("als", "Als", PlaceKind.ISLAND, parentId = "dk", matchNames = emptyList()),
        ),
        overrides: List<LocalOverride> = listOf(LocalOverride("raev", "als", emptyList(), null)),
        source: String = law,
    ) = HuntingCatalog(
        version = 1,
        updated = "1. juli 2024",
        sourceName = "BEK 470/2024",
        sourceURL = source,
        groups = if (species.isEmpty()) emptyList() else listOf(SpeciesGroup("pattedyr", "Pattedyr", species)),
        places = places,
        overrides = overrides,
    )

    @Test
    fun bundledDataPasses() {
        assertEquals(emptyList(), bundledCatalog().problems)
    }

    @Test
    fun soundCatalogHasNoProblems() {
        assertEquals(emptyList(), catalog().problems)
    }

    @Test
    fun catalogWithoutSpeciesIsRefused() {
        assertEquals(listOf(CatalogProblem.NoSpecies), catalog(species = emptyList(), overrides = emptyList()).problems)
    }

    @Test
    fun catalogWithoutDenmarkIsRefused() {
        assertEquals(listOf(CatalogProblem.NoDenmark), catalog(places = emptyList(), overrides = emptyList()).problems)
    }

    @Test
    fun repeatedSpeciesIsRefused() {
        val fox = Species("raev", "Ræv", emptyList(), null)
        assertEquals(listOf(CatalogProblem.RepeatedSpecies("raev")), catalog(species = listOf(fox, fox)).problems)
    }

    @Test
    fun repeatedPlaceIsRefused() {
        val places = listOf(
            Place("dk", "Danmark", PlaceKind.COUNTRY, matchNames = emptyList()),
            Place("als", "Als", PlaceKind.ISLAND, parentId = "dk", matchNames = emptyList()),
            Place("als", "Als igen", PlaceKind.ISLAND, parentId = "dk", matchNames = emptyList()),
        )
        assertEquals(listOf(CatalogProblem.RepeatedPlace("als")), catalog(places = places).problems)
    }

    @Test
    fun placeUnderUnknownParentIsRefused() {
        val places = listOf(
            Place("dk", "Danmark", PlaceKind.COUNTRY, matchNames = emptyList()),
            Place("als", "Als", PlaceKind.ISLAND, parentId = "syddanmark", matchNames = emptyList()),
        )
        assertEquals(
            listOf(CatalogProblem.UnknownParent("als", "syddanmark")),
            catalog(places = places).problems,
        )
    }

    @Test
    fun overrideForUnknownSpeciesIsRefused() {
        val overrides = listOf(LocalOverride("ulv", "als", emptyList(), null))
        assertEquals(listOf(CatalogProblem.OverrideForUnknownSpecies("ulv")), catalog(overrides = overrides).problems)
    }

    @Test
    fun overrideForUnknownPlaceIsRefused() {
        val overrides = listOf(LocalOverride("raev", "anholt", emptyList(), null))
        assertEquals(listOf(CatalogProblem.OverrideForUnknownPlace("anholt")), catalog(overrides = overrides).problems)
    }

    @Test
    fun sourceOutsideRetsinformationIsRefused() {
        val sources = listOf(
            "http://www.retsinformation.dk/eli/lta/2024/470",
            "https://retsinformation.dk.example.com/eli/lta/2024/470",
            "https://www.retsinformation.dk@example.com/eli",
            "https://example.com/?u=https://www.retsinformation.dk",
            "jagtfokus://open",
            "javascript:alert(1)",
            "",
        )
        for (source in sources) {
            assertEquals(listOf(CatalogProblem.UntrustedSource(source)), catalog(source = source).problems, source)
            assertNull(catalog(source = source).sourceLink, source)
        }
    }

    @Test
    fun sourceOnRetsinformationIsALink() {
        val sources = listOf(
            "https://www.retsinformation.dk/eli/lta/2024/470",
            "https://retsinformation.dk/eli/lta/2025/12",
            "https://WWW.Retsinformation.DK/eli/lta/2024/470",
        )
        for (source in sources) {
            assertEquals(emptyList(), catalog(source = source).problems, source)
            assertEquals(source, catalog(source = source).sourceLink, source)
        }
    }

    @Test
    fun decodeCheckedRefusesAFileWithProblems() {
        val data = HuntingCatalog.encode(catalog(species = emptyList(), overrides = emptyList()))
        val error = assertFailsWith<CatalogRefused> { decodeChecked(data) }
        assertEquals(listOf(CatalogProblem.NoSpecies), error.problems)
    }

    @Test
    fun decodeCheckedReturnsASoundFile() {
        val sound = catalog()
        assertEquals(sound, decodeChecked(HuntingCatalog.encode(sound)))
    }

    @Test
    fun bundledFileDecodesChecked() {
        val file = java.io.File("data/jagttider.json").readBytes()
        assertTrue(decodeChecked(file).problems.isEmpty())
    }
}
