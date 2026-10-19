package dk.jagtfokus.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** Ports SpeciesBiologyTests.swift. Reads the bundled data and the iOS repo's biology file. */
class SpeciesBiologyTest {
    private val catalog = bundledCatalog()
    private val biology = BiologyCatalog.decode(repoFile("data/biologi.json").readBytes())

    @Test
    fun everySpeciesHasBiology() {
        val missing = catalog.allSpecies.map { it.id }.filter { biology.biology(it) == null }
        assertTrue(missing.isEmpty(), "Mangler biologi: $missing")
    }

    @Test
    fun noStaleSpeciesIds() {
        val known = catalog.allSpecies.map { it.id }.toSet()
        val stale = biology.species.keys.filter { it !in known }
        assertTrue(stale.isEmpty(), "Ukendte art-id: $stale")
    }

    @Test
    fun everyTaxonIsUsed() {
        val used = biology.species.values.toSet()
        assertEquals(used, biology.taxa.keys)
    }

    @Test
    fun sexClassesShareTheSpecies() {
        assertEquals(biology.species["kronhjort"], biology.species["kronhind"])
        assertEquals(biology.species["raabuk"], biology.species["raa_og_lam"])
        assertEquals(biology.species["graagaas"], biology.species["graagaas_omdrift"])
        assertEquals("Somateria mollissima", biology.biology("edderfugl_han")?.latin)
    }

    /** Life history only. No stock counts that go stale, and no weak hosts. */
    @Test
    fun factsAreSourcedAndStable() {
        val allowedHosts = listOf("dofbasen.dk", "arter.dk", "lex.dk", "sgavmst.dk")
        val invasiveURL = "https://sgavmst.dk/natur-og-jagt/regulering-og-udsaetning-af-vildt/regulering-af-vildt/regulering-af-invasive-arter"
        for ((id, taxon) in biology.taxa) {
            assertTrue(taxon.latin.isNotEmpty(), id)
            assertEquals(listOf("I Danmark", "Yngel"), taxon.facts.map { it.label }, id)
            assertTrue(taxon.sources.isNotEmpty(), id)
            for (fact in taxon.facts) {
                assertTrue(fact.value.isNotEmpty(), "$id ${fact.label}")
                assertEquals(fact.value.trim(), fact.value, id)
                assertTrue("Wikipedia" !in fact.value, id)
            }
            val hosts = taxon.sources.map { source ->
                assertTrue(source.name.isNotEmpty(), id)
                assertTrue(source.url.startsWith("https://"), "$id ${source.url}")
                val host = java.net.URI(source.url).host ?: ""
                assertTrue(allowedHosts.any { host == it || host.endsWith(".$it") }, "$id $host")
                source.url
            }
            if (taxon.facts.any { "Invasiv" in it.value }) {
                assertTrue(invasiveURL in hosts, "$id siger invasiv uden myndighedskilde")
            }
        }
    }
}
