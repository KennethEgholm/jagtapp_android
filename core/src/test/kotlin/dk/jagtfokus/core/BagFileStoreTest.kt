package dk.jagtfokus.core

import java.io.File
import java.nio.file.Files
import java.time.Instant
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class BagFileStoreTest {
    private val dir: File = Files.createTempDirectory("bagstore").toFile()
    private val main = File(dir, "vildtudbytte.json")
    private val backup = File(dir, "vildtudbytte.backup.json")
    private val corrupt = File(dir, "vildtudbytte.corrupt.json")
    private val now = Instant.parse("2026-10-19T10:00:00Z")

    @AfterTest
    fun cleanUp() {
        dir.deleteRecursively()
    }

    private fun bag(vararg speciesIds: String): BagJournal =
        speciesIds.fold(BagJournal()) { acc, id -> acc.record(id, day(2026, 10, 19), null, now) }

    private fun ids(journal: BagJournal) = journal.entries.map { it.speciesId }

    @Test
    fun saveThenLoadRoundTrips() {
        val store = BagFileStore(main)
        store.save(bag("raev", "hare"))
        assertEquals(listOf("raev", "hare"), ids(BagFileStore(main).load()))
    }

    @Test
    fun missingFileLoadsEmpty() {
        assertTrue(BagFileStore(main).load().entries.isEmpty())
    }

    @Test
    fun backupHoldsThePreviousGoodVersion() {
        val store = BagFileStore(main)
        store.save(bag("raev"))
        store.save(bag("raev", "hare"))
        assertEquals(listOf("raev"), ids(BagJournal.decode(backup.readBytes())))
        assertEquals(listOf("raev", "hare"), ids(BagJournal.decode(main.readBytes())))
    }

    @Test
    fun corruptMainIsPreservedAndBackupRestored() {
        val store = BagFileStore(main)
        store.save(bag("raev"))
        store.save(bag("raev", "hare"))
        main.writeText("{ not json")

        val loaded = BagFileStore(main).load()

        assertEquals(listOf("raev"), ids(loaded))
        assertEquals("{ not json", corrupt.readText())
        assertEquals(listOf("raev"), ids(BagJournal.decode(main.readBytes())))
    }

    @Test
    fun corruptMainIsPreservedOnSaveAndBackupKeepsLastGood() {
        val store = BagFileStore(main)
        store.save(bag("raev"))
        store.save(bag("raev", "hare"))
        main.writeText("garbage")
        store.save(bag("fasan"))

        assertEquals("garbage", corrupt.readText())
        assertEquals(listOf("raev"), ids(BagJournal.decode(backup.readBytes())))
        assertEquals(listOf("fasan"), ids(BagFileStore(main).load()))
    }

    @Test
    fun truncatedTempFileNeverReplacesTheMainFile() {
        val store = BagFileStore(main)
        store.save(bag("raev"))
        File(dir, "vildtudbytte.json.tmp").writeText("{\"entr")

        assertEquals(listOf("raev"), ids(BagFileStore(main).load()))

        store.save(bag("raev", "hare"))
        assertEquals(listOf("raev", "hare"), ids(BagFileStore(main).load()))
        assertFalse(File(dir, "vildtudbytte.json.tmp").exists())
    }

    @Test
    fun speciesListRoundTripsWithBackupAndRestore() {
        val file = File(dir, "artsliste.json")
        val store = SpeciesChecklistFileStore(file)
        assertFalse(store.fileExists)
        assertNull(store.load())

        store.save(SpeciesChecklist().add("raev"))
        store.save(SpeciesChecklist().add("raev").add("hare"))
        assertTrue(store.fileExists)
        assertTrue(File(dir, "artsliste.backup.json").isFile)

        file.writeText("nope")
        val restored = SpeciesChecklistFileStore(file).load()

        assertEquals(SpeciesChecklist().add("raev"), restored)
        assertEquals("nope", File(dir, "artsliste.corrupt.json").readText())
    }
}
