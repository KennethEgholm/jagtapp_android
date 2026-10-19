package dk.jagtfokus.app.data

import dk.jagtfokus.core.BagJournal
import dk.jagtfokus.core.SpeciesChecklist
import java.io.File

/**
 * Safe local storage with rolling backup and corrupt-file preservation.
 * Port of BagFileStore.swift. The file stays on the phone; no cloud sync.
 */
class BagFileStore(fileURL: File) {
    private val backupURL = File(fileURL.parentFile, fileURL.nameWithoutExtension + ".backup.json")
    private val corruptURL = File(fileURL.parentFile, fileURL.nameWithoutExtension + ".corrupt.json")
    private val file = fileURL

    fun load(): BagJournal {
        if (file.isFile) {
            runCatching {
                val data = file.readBytes()
                val journal = BagJournal.decode(data)
                return journal
            }.onFailure { preserveCorruptFile() }
        }
        runCatching {
            val backupData = backupURL.readBytes()
            val backupJournal = BagJournal.decode(backupData)
            file.writeBytes(backupData)
            return backupJournal
        }
        return BagJournal()
    }

    fun save(journal: BagJournal) {
        file.parentFile?.mkdirs()
        if (file.isFile) {
            runCatching {
                val existing = file.readBytes()
                BagJournal.decode(existing)
                backupURL.writeBytes(existing)
            }.onFailure { preserveCorruptFile() }
        }
        file.writeBytes(BagJournal.encode(journal))
    }

    private fun preserveCorruptFile() {
        runCatching { corruptURL.writeBytes(file.readBytes()) }
    }
}

/** Species list next to the bag file, same naming as iOS: artsliste.json. */
class SpeciesChecklistFileStore(fileURL: File) {
    private val file = fileURL

    val fileExists: Boolean
        get() = file.isFile

    fun load(): SpeciesChecklist? =
        runCatching { SpeciesChecklist.decode(file.readBytes()) }.getOrNull()

    fun save(list: SpeciesChecklist) {
        file.parentFile?.mkdirs()
        file.writeBytes(SpeciesChecklist.encode(list))
    }
}
